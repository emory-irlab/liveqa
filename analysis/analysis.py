from __future__ import print_function

import csv, sqlite3
from scipy import stats

BASE_PATH = "/home/dsavenk/Projects/octiron/data/liveqa/liveqa_16/run_logs/"
CROWD_LOG_PATH = BASE_PATH + "liveqa-crowd.log"
DB_PATH = (BASE_PATH + "crowd.db", BASE_PATH + "crowd_2.db")
QNA_PATH = BASE_PATH + "qna_06022016_3_330pm.txt"
CSV_TOJUDGE_PATH = BASE_PATH + "answers_to_judge.csv"
RATINGS_PATH = BASE_PATH + "judged_answers.csv"


def annalyze_log():
    with open(CROWD_LOG_PATH, 'r') as inp:
        for line in inp:
            pass


def read_qna():
    qnas = dict()
    with open(QNA_PATH, 'r') as inp:
        missing = 0
        for line in inp:
            fields = line.split('\t')
            qid = fields[0]
            if not qid:
                missing += 1
                continue
            title = fields[1]
            body = fields[2]
            bestAnswer = fields[3]
            categories = fields[4].split(',')
            answers = [x.strip() for x in fields[5:] if x.strip()]
            if not bestAnswer and answers:
                bestAnswer = answers[0]
            if not bestAnswer:
                missing += 1
            qnas[qid] = (title, body, bestAnswer, categories, answers)
        print(missing)
    return qnas


def analyze_db():
    import sqlite3
    from random import shuffle
    import csv

    with open(CSV_TOJUDGE_PATH, 'w+') as csvfile:
        fieldnames = ['qid', 'title', 'body', 'category',
                      'answer_1', 'answer_1_source', "answer_1_id",
                      'answer_2', 'answer_2_source', "answer_2_id",
                      'answer_3', 'answer_3_source', "answer_3_id",
                      'answer_4', 'answer_4_source', "answer_4_id",
                      'answer_5', 'answer_5_source', "answer_5_id",
                      'answer_6', 'answer_6_source', "answer_6_id",
                      'answer_7', 'answer_7_source', "answer_7_id",
                      'answer_8', 'answer_8_source', "answer_8_id",
                      'answer_9', 'answer_9_source', "answer_9_id"]
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()

        qnas = read_qna()

        for qid, qna in qnas.iteritems():
            answers = [('YAHOO', -1, qna[2])]
            for db in DB_PATH:
                conn = sqlite3.connect(db)
                c = conn.cursor()
                res = c.execute("SELECT ID, ANSWER FROM answers WHERE QID = '%s'" % ('YA:' + qid))
                for line in res.fetchall():
                    answers.append(('DB', line[0], line[1].encode('utf-8')))
                conn.close()
            shuffle(answers)
            row = {"qid": qid,
                   "title": qna[0],
                   "body": qna[1],
                   "category": " >> ".join(qna[3])}
            for i in range(1, 10):
                answer = answers[i-1] if i <= len(answers) else ("NONE", "", "")
                row['answer_' + str(i)] = answer[2]
                row['answer_' + str(i) + "_source"] = answer[0]
                row['answer_' + str(i) + "_id"] = answer[1]
            writer.writerow(row)


def analyze_ratings():
    import csv
    res = dict()
    auto_scores = []
    with_crowd_scores = []
    yahoo_scores = []
    user_generated_scores = []
    all_auto_scores = []
    with open(RATINGS_PATH, 'r') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            qid = row['Input.qid']
            if qid not in res:
                res[qid] = dict()
            for i in range(1, 10):
                answer = row['Input.answer_' + str(i)]
                source = row['Input.answer_' + str(i) + '_source']
                if not row['Input.answer_' + str(i) + '_id']:
                    continue
                id = int(row['Input.answer_' + str(i) + '_id'])
                rating = row['Answer.rating_' + str(i)]
                useful = row['Answer.useful_' + str(i)]
                if id not in res[qid]:
                    res[qid][id] = []
                res[qid][id].append((rating, useful, source, answer))

    failed_count = 0
    for qid, answers in res.iteritems():
        best_answer = get_best_answer_id(qid)
        best_crowd_answer = get_best_crowd_answer_id(qid)
        user_generated_ids = get_user_generated_ids(qid)

        # if best_answer not in answers or best_crowd_answer not in answers:
        #     failed_count += 1
        #     continue

        for aid, ratings in answers.iteritems():
            if aid == best_answer:
                auto_scores.extend([float(x[0]) for x in ratings])
                # auto_scores.append(1.0 * sum([float(x[0]) for x in ratings]) / len(ratings))
            if aid == best_crowd_answer:
                with_crowd_scores.extend([float(x[0]) for x in ratings])
                #with_crowd_scores.append(1.0 * sum([float(x[0]) for x in ratings]) / len(ratings))
            if ratings[0][2] == 'YAHOO':
                yahoo_scores.extend([float(x[0]) for x in ratings])
            if aid in user_generated_ids:
                user_generated_scores.extend([float(x[0]) for x in ratings])
            else:
                all_auto_scores.extend([float(x[0]) for x in ratings])

    import matplotlib.pyplot as plt
    import numpy as np
    import seaborn as sns

    sns.distplot(auto_scores, label='Automated system', hist_kws={"alpha": 0.5})
    sns.distplot(with_crowd_scores, label='Automated system with crowdsourcing', hist_kws={"alpha": 0.5})
    plt.title("Gaussian Histogram")
    plt.xlabel("Value")
    plt.ylabel("Frequency")
    plt.legend()
    plt.show()


# print(failed_count)
    print(1.0 * sum(auto_scores) / len(auto_scores))
    print(1.0 * sum(with_crowd_scores) / len(with_crowd_scores))
    print(stats.ttest_ind(auto_scores, with_crowd_scores))
    print(1.0 * sum(yahoo_scores) / len(yahoo_scores))
    print(1.0 * sum(user_generated_scores) / len(user_generated_scores))
    print(1.0 * sum(all_auto_scores) / len(all_auto_scores))


def get_best_answer_id(qid):
    for db in DB_PATH:
        conn = sqlite3.connect(db)
        c = conn.cursor()
        res = c.execute("SELECT ID FROM answers WHERE QID = '%s' AND RANK = 6" % ('YA:' + qid)).fetchall()
        if res:
            conn.close()
            return res[0][0]
        conn.close()
    return -1

def get_best_crowd_answer_id(qid):
    for db in DB_PATH:
        conn = sqlite3.connect(db)
        c = conn.cursor()
        res = c.execute("SELECT answers.ID, answers.ANSWER, answers.RANK, AVG(ratings.RATING) FROM answers LEFT JOIN ratings ON answers.ID = ratings.AID WHERE answers.QID = '%s' GROUP BY answers.ID" % ('YA:' + qid)).fetchall()
        if res:
            conn.close()

            res = sorted(res, cmp=lambda a, b: sign(safe_diff(a[3], b[3]) if (a[3] != b[3]) else rank_diff(a[2], b[2])), reverse=True)
            if res[0][3] >= 3.0:
                return res[0][0]
            user_answer = ""
            user_answer_id = None
            for answer in res:
                if answer[2] == -1 and len(answer[1]) > len(user_answer):
                    user_answer = answer[1]
                    user_answer_id = answer[0]
            if user_answer_id:
                return user_answer_id
            return res[0][0]

        conn.close()
    return -1

def get_user_generated_ids(qid):
    for db in DB_PATH:
        conn = sqlite3.connect(db)
        c = conn.cursor()
        res = c.execute("SELECT answers.ID FROM answers WHERE QID = '%s' AND RANK = -1" % ('YA:' + qid)).fetchall()
        if res:
            conn.close()
            return set([r[0] for r in res])

        conn.close()
    return set()


def sign(val):
    return (val > 0) - (val < 0)

def safe_diff(val1, val2):
    return (val1 if val1 else 0.0) - (val2 if val2 else 0.0)

def rank_diff(rank1, rank2):
    if rank1 == -1:
        rank1 = 10
    if rank2 == -1:
        rank2 = 10
    return sign(rank2 - rank1)


def analysis():
    sum =
    for db in DB_PATH:
        conn = sqlite3.connect(db)
        c = conn.cursor()
        res = c.execute("SELECT COUNT(*) FROM answers WHERE WORKERID != ''").fetchall()
        conn.close()
        if res:
            print(res[0][0])
    return -1

if __name__ == "__main__":
    # analyze_ratings()
    # read_qna()
    analysis()


