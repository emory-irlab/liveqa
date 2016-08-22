
import csv, random, sqlite3

import hashlib
from sys import argv

from math import sqrt
import numpy as np
import pandas as pd
from scipy import stats
import seaborn as sns
from sklearn.ensemble import GradientBoostingRegressor


# BASE_PATH = "/home/dsavenk/data/liveqa/liveqa_16/run_logs/"
BASE_PATH = "/home/dsavenk/Mounts/octiron/data/liveqa/liveqa_16/run_logs/"

THRESHOLD = -1

def read_ratings(rating_file):
    question_ratings = dict()
    questions_by_qid = dict()
    categories = set()
    with open(rating_file, 'r') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            qid = row['Input.qid']
            category = row['Input.category'].split(" >> ")[0]
            questions_by_qid[qid] = (row['Input.title'] + " " + row['Input.body'], category)
            categories.add(category)
            if qid not in question_ratings:
                question_ratings[qid] = dict()
            for i in range(1, 10):
                if not row['Input.answer_' + str(i) + '_id']:
                    continue

                id = int(row['Input.answer_' + str(i) + '_id'])
                source = row['Input.answer_' + str(i) + '_source']
                rating = int(row['Answer.rating_' + str(i)])
                useful = row['Answer.useful_' + str(i)]
                text = row['Input.answer_' + str(i)]
                if id not in question_ratings[qid]:
                    question_ratings[qid][id] = []
                question_ratings[qid][id].append((source, rating, useful, text))
    print categories
    return question_ratings, questions_by_qid


def split_train_test(ratings, train_fraction=0.3):
    train_ratings = dict()
    test_ratings = dict()
    for qid in ratings.iterkeys():
        if random.random() < train_fraction:
            train_ratings[qid] = ratings[qid]
        else:
            test_ratings[qid] = ratings[qid]
    return train_ratings, test_ratings


def get_question(qid, db_paths=(BASE_PATH + "crowd.db", BASE_PATH + "crowd_2.db")):
    for db in db_paths:
        conn = sqlite3.connect(db)
        c = conn.cursor()
        res = c.execute("SELECT TITLE, BODY, CATEGORY FROM QUESTIONS WHERE QID = '%s'" % ('YA:' + qid)).fetchall()
        if res:
            conn.close()
            return res
        conn.close()
    return []


_answers_cache = None
def get_question_answers(qid, db_paths=(BASE_PATH + "crowd.db", BASE_PATH + "crowd_2.db"),
                         filter_answer=lambda r: r[4] == '' or int(hashlib.md5(r[4]).hexdigest(), 16) % 10 > THRESHOLD):
    global _answers_cache
    qid = 'YA:' + qid
    if not _answers_cache:
        _answers_cache = dict()
        for db in db_paths:
            conn = sqlite3.connect(db)
            c = conn.cursor()
            res = c.execute(
                "SELECT QID, ID, ANSWER, RANK, ANSWER_TYPE, WORKERID FROM ANSWERS").fetchall()
            for r in res:
                cur_qid = r[0]
                r = r[1:]
                if cur_qid not in _answers_cache:
                    _answers_cache[cur_qid] = []
                if filter_answer(r):
                    _answers_cache[cur_qid].append(r)
            conn.close()
    return _answers_cache[qid] if qid in _answers_cache else []

# def get_answer_ratings(qid, aid, db_paths=(BASE_PATH + "crowd.db", BASE_PATH + "crowd_2.db"),
#                        filter_rating=lambda r: r[2] == 0):
#     for db in db_paths:
#         conn = sqlite3.connect(db)
#         c = conn.cursor()
#         res = c.execute("SELECT ratings.WORKER, ratings.RATING, answers_received.SHUFFLED FROM ratings "
#                         "LEFT JOIN answers ON answers.ID = ratings.AID "
#                         "LEFT JOIN answers_received ON ratings.WORKER = answers_received.WORKER "
#                         "WHERE answers.ID = %d AND answers.QID = 'YA:%s' AND answers_received.qid = 'YA:%s'" % (aid, qid, qid)).fetchall()
#         if res:
#             conn.close()
#             return [r for r in res if filter_rating(r)]
#         conn.close()
#     return []

_ratings_cache = None
def get_answer_ratings(qid, aid, db_paths=(BASE_PATH + "crowd.db", BASE_PATH + "crowd_2.db"),
                       filter_rating=lambda r: int(hashlib.md5(r[0]).hexdigest(), 16) % 10 > THRESHOLD):
                        #filter_rating=lambda r: r[2] == 0):
    global _ratings_cache
    qid = 'YA:' + qid
    if not _ratings_cache:
        _ratings_cache = dict()
        for db in db_paths:
            conn = sqlite3.connect(db)
            c = conn.cursor()
            res = c.execute("SELECT answers.ID, answers.QID, ratings.WORKER, ratings.RATING, answers_received.SHUFFLED FROM ratings "
                            "LEFT JOIN answers ON answers.ID = ratings.AID "
                            "LEFT JOIN answers_received ON ratings.WORKER = answers_received.WORKER AND answers.QID = answers_received.QID "
                            ).fetchall()
            for r in res:
                aid = r[0]
                qid = r[1]
                r = r[2:]
                if (qid, aid) not in _ratings_cache:
                    _ratings_cache[(qid, aid)] = []
                if filter_rating(r):
                    _ratings_cache[(qid, aid)].append(r)
            conn.close()
    return _ratings_cache[(qid, aid)] if (qid, aid) in _ratings_cache else []


def generate_features(qid, answers, include_crowd_answers=True, include_weights=True):
    features = []
    labels = []
    # question = get_question(qid)
    answers_data = dict([(answer[0], answer[1:]) for answer in get_question_answers(qid)])
    index = -1
    indexes = set()
    for aid, ratings in answers:
        index += 1
        if aid not in answers_data or aid == -1 or (not include_crowd_answers and answers_data[aid][2] == 4):
            continue
        worker_ratings = [r[1] for r in get_answer_ratings(qid, aid)]

        indexes.add(index)
        labels.append(1.0 * sum([rating[1] for rating in ratings]) / len(ratings))

        if not include_weights:
            worker_ratings = []

        features.append([
            answers_data[aid][1],  # rank
            1.0 * sum(worker_ratings) / len(worker_ratings) if worker_ratings else 0.0,  # avg worker rating
            min(worker_ratings) if worker_ratings else 0.0,  # min worker rating
            max(worker_ratings) if worker_ratings else 0.0,  # max worker rating
            sorted(worker_ratings)[len(worker_ratings) / 2] if worker_ratings else 0.0,  # median worker rating
            0 if worker_ratings else 1,  # no worker ratings
            len(worker_ratings),         # number of worker ratings
            len(answers_data[aid][0]),   # answer length
            1.0 if answers_data[aid][2] == 1 else 0.0,   # YAHOO_ANSWERS
            1.0 if answers_data[aid][2] == 2 else 0.0,   # WEB
            1.0 if answers_data[aid][2] == 3 else 0.0,   # ANSWERS_COM
            1.0 if answers_data[aid][2] == 4 else 0.0,   # CROWD
            1.0 if answers_data[aid][2] == -1 else 0.0   # WIKIHOW
        ])
    return features, labels, indexes


def create_dataset(ratings, include_crowd_answers=True, include_weights=True):
    features = []
    labels = []
    for qid, answers in ratings.iteritems():
        f, l, _ = generate_features(qid, answers.items(), include_crowd_answers, include_weights)
        features.extend(f)
        labels.extend(l)
    return features, labels


def train_model(features, labels):
    regressor = GradientBoostingRegressor(n_estimators=100, max_depth=3, subsample=0.8, learning_rate=0.1)
    return regressor.fit(features, labels)


def test_model(model, test_ratings, include_crowd_answers=True, include_weights=True, qid2title=None, category=None):
    original_scores = []
    scores = []
    heuristic_scores = []
    rating_scores = []
    yahoo_scores = []
    number_of_questions = 0

    questions_score_diff = []

    for qid, answers in test_ratings.iteritems():
        if category is not None and qid2title is not None and qid2title[qid][1] != category:
            continue

        number_of_questions += 1
        answers = answers.items()
        ya_answers = [ratings for aid, ratings in answers if aid == -1]
        yahoo_scores.append((1.0 * sum([r[1] for r in ya_answers[0]]) / len(ya_answers[0])) if ya_answers else 0.0)

        # Update answers to include only system answers.
        features, labels, indexes = generate_features(qid, answers, include_crowd_answers, include_weights)
        answers = [aid_ratings for index, aid_ratings in enumerate(answers) if index in indexes]

        if not answers:
            scores.append(0.0)
            original_scores.append(0.0)
            heuristic_scores.append(0.0)
            rating_scores.append(0.0)
        else:
            predictions = model.predict(features)
            original = [ans for ans in sorted(enumerate(answers), key=lambda a:features[a[0]][0]) if features[ans[0]][0] != -1]
            heuristic = sorted(enumerate(answers), key=lambda a: features[a[0]][1], reverse=True)
            reranking = sorted(enumerate(answers), key=lambda a: predictions[a[0]], reverse=True)
            scores.append(labels[reranking[0][0]])
            original_scores.append(labels[original[0][0]] if original else 0.0)
            rating_scores.append(labels[heuristic[0][0]])

            if features[heuristic[0][0]][1] >= 2.5:
                heuristic_scores.append(labels[heuristic[0][0]])
            else:
                worker_answer = -1
                worker_answer_length = 0
                for a in heuristic:
                    if features[a[0]][12] == 1 and worker_answer_length < features[a[0]][7]:
                        worker_answer = a[0]
                        worker_answer_length = features[a[0]][7]
                if worker_answer != -1:
                    heuristic_scores.append(labels[worker_answer])
                else:
                    heuristic_scores.append(labels[heuristic[0][0]])
        questions_score_diff.append((original_scores[-1], scores[-1], (qid2title[qid] if qid2title is not None else qid, original[0][1][1][0][3] if original else "",
            features[original[0][0]] if original else [], reranking[0][1][1][0][3] if reranking else "", features[reranking[0][0]] if reranking else [])))

    sns.set_context("notebook", font_scale=2, rc={"lines.linewidth": 2.5})
    sns.set_style("white")
    sns.set_style("ticks")
    #sns.set_palette("bright")
    sns.distplot(original_scores, hist_kws={"alpha": 0.3, "hatch": "//", "label": "Original ranking"}, bins=[0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4], kde_kws={"label": "Original ranking", "linestyle": "-.", "lw": 5})
    sns.distplot(scores, hist_kws={"alpha": 0.3, "hatch":"o", "label": "CRQA"}, bins=[0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4], kde_kws={"label": "CRQA", "linestyle": "solid", "lw": 5})
    hist = sns.distplot(yahoo_scores, hist_kws={"alpha": 0.3,  "hatch":"\\", "label": "Yahoo! Answers"}, bins=[0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4], kde_kws={"label": "Yahoo! Answers", "linestyle": "dashed", "lw": 5})
    hist.set(xlim=(0, 4))
    sns.plt.legend(loc='upper left')
    sns.plt.xlabel("Answer score")
    sns.plt.show()

    import operator
    questions_score_diff.sort(key=lambda x: x[0] - x[1])

    # for o, s, question in questions_score_diff:
    #     print "-------------------"
    #     title, original, ofeats, reranking, refeats = question
    #     print title
    #     print "\t\t", o, "\t", original.replace("\n", " ")
    #     print "\t\t\t", ofeats
    #     print "\t\t", s, "\t", reranking.replace("\n", " ")
    #     print "\t\t\t", refeats

    accuracy = {
        "Original ranking = ": get_accuracy(original_scores, number_of_questions),     # stats.ttest_rel(original_scores, scores)[1]
        "Worker rating = ": get_accuracy(rating_scores, number_of_questions),
        "My heuristic = ": get_accuracy(heuristic_scores, number_of_questions),
        "Reranking model = ": get_accuracy(scores, number_of_questions),
        "Yahoo! Answers = ": get_accuracy(yahoo_scores, number_of_questions),
    }

    precision = {
        "Original ranking = ": get_precision(sum(original_scores), original_scores),
        "Worker rating = ": get_precision(sum(rating_scores), rating_scores),
        "My heuristic = ": get_precision(sum(heuristic_scores), heuristic_scores),
        "Reranking model = ": get_precision(sum(scores), scores),
        "Yahoo! Answers = ": get_precision(sum(yahoo_scores), yahoo_scores)
    }
    
    suc2 = {"Original ranking = ": get_suc_k(original_scores, 2, number_of_questions),
            "Worker rating = ": get_suc_k(rating_scores, 2, number_of_questions),
            "My heuristic = ": get_suc_k(heuristic_scores, 2, number_of_questions),
            "Reranking model = ": get_suc_k(scores, 2, number_of_questions),
            "Yahoo! Answers = ": get_suc_k(yahoo_scores, 2, number_of_questions)}
    suc3 = {"Original ranking = ": get_suc_k(original_scores, 3, number_of_questions),
            "Worker rating = ": get_suc_k(rating_scores, 3, number_of_questions),
            "My heuristic = ": get_suc_k(heuristic_scores, 3, number_of_questions),
            "Reranking model = ": get_suc_k(scores, 3, number_of_questions),
            "Yahoo! Answers = ": get_suc_k(yahoo_scores, 3, number_of_questions)}
    suc4 = {"Original ranking = ": get_suc_k(original_scores, 4, number_of_questions),
            "Worker rating = ": get_suc_k(rating_scores, 4, number_of_questions),
            "My heuristic = ": get_suc_k(heuristic_scores, 4, number_of_questions),
            "Reranking model = ": get_suc_k(scores, 4, number_of_questions),
            "Yahoo! Answers = ": get_suc_k(yahoo_scores, 4, number_of_questions)}
    prec2 = {"Original ranking = ": get_prec_k(original_scores, 2),
            "Worker rating = ": get_prec_k(rating_scores, 2),
            "My heuristic = ": get_prec_k(heuristic_scores, 2),
            "Reranking model = ": get_prec_k(scores, 2),
            "Yahoo! Answers = ": get_prec_k(yahoo_scores, 2)}
    prec3 = {"Original ranking = ": get_prec_k(original_scores, 3),
            "Worker rating = ": get_prec_k(rating_scores, 3),
            "My heuristic = ": get_prec_k(heuristic_scores, 3),
            "Reranking model = ": get_prec_k(scores, 3),
            "Yahoo! Answers = ": get_prec_k(yahoo_scores, 3)}
    prec4 = {"Original ranking = ": get_prec_k(original_scores, 4),
            "Worker rating = ": get_prec_k(rating_scores, 4),
            "My heuristic = ": get_prec_k(heuristic_scores, 4),
            "Reranking model = ": get_prec_k(scores, 4),
            "Yahoo! Answers = ": get_prec_k(yahoo_scores, 4)}

    # print "------------------ ACCURACY -----------------"
    # print "Original ranking = ", accuracy["Original ranking = "]
    # print "Worker rating = ", accuracy["Worker rating = "], stats.ttest_rel(original_scores, rating_scores)[1]
    # print "My heuristic = ", accuracy["My heuristic = "], stats.ttest_rel(original_scores, heuristic_scores)[1]
    # print "Reranking model = ", accuracy["Reranking model = "], stats.ttest_rel(original_scores, scores)[1]
    # print "Yahoo! Answers = ", accuracy["Yahoo! Answers = "], stats.ttest_rel(original_scores, yahoo_scores)[1]
    # print "------------------ PRECISION -----------------"
    # print "Original ranking = ", precision["Original ranking = "]
    # print "Worker rating = ", precision["Worker rating = "]
    # print "My heuristic = ", precision["My heuristic = "]
    # print "Reranking model = ", precision["Reranking model = "]
    # print "Yahoo! Answers = ", precision["Yahoo! Answers = "]
    # print "------------------ SUCC@k+ -----------------"
    # print "Original ranking = ", suc2["Original ranking = "], suc3["Original ranking = "], suc4["Original ranking = "]
    # print "Worker rating = ", suc2["Worker rating = "], suc3["Worker rating = "], suc4["Worker rating = "]
    # print "My heuristic = ", suc2["My heuristic = "], suc3["My heuristic = "], suc4["My heuristic = "]
    # print "Reranking model = ", suc2["Reranking model = "], suc3["Reranking model = "], suc4["Reranking model = "]
    # print "Yahoo! Answers = ", suc2["Yahoo! Answers = "], suc3["Yahoo! Answers = "], suc4["Yahoo! Answers = "]
    # print "------------------ PREC@k+ -----------------"
    # print "Original ranking = ", prec2["Original ranking = "], prec3["Original ranking = "], prec4["Original ranking = "]
    # print "Worker rating = ", prec2["Worker rating = "], prec3["Worker rating = "], prec4["Worker rating = "]
    # print "My heuristic = ", prec2["My heuristic = "], prec3["My heuristic = "], prec4["My heuristic = "]
    # print "Reranking model = ", prec2["Reranking model = "], prec3["Reranking model = "], prec4["Reranking model = "]
    # print "Yahoo! Answers = ", prec2["Yahoo! Answers = "], prec3["Yahoo! Answers = "], prec4["Yahoo! Answers = "]
    return accuracy, precision, suc2, suc3, suc4, prec2, prec3, prec4, number_of_questions


def get_prec_k(scores, k):
    return sum([1.0 for score in scores if score >= k]) / len([score for score in scores if score > 0.5]) if scores else 0.0


def get_suc_k(scores, k, number_of_questions):
    return sum([1.0 for score in scores if score >= k]) / number_of_questions  if scores else 0.0


def get_accuracy(scores, number_of_questions):
    return sum([score for score in scores if score > 0]) / number_of_questions  if scores else 0.0


def get_precision(nom, list):
    denom = len([x for x in list if x > 0.5])
    return 1.0 * nom / denom if denom != 0 else 0.0


if __name__ == "__main__":
    random.seed(42)
    include_crowd_answers = True
    include_weights = True
    ratings, qid2title = read_ratings(argv[1])

    accuracies = dict()
    precisions = dict()
    succ2 = dict()
    succ3 = dict()
    succ4 = dict()
    prec2 = dict()
    prec3 = dict()
    prec4 = dict()

    sns.set(style="ticks")

    # thresholds = []
    # typ = []
    # scores = []


    # for th in range(-1, 10):
    #     _ratings_cache = None
    #     _answers_cache = None
    #     THRESHOLD = th
    #     for i in xrange(50):
    #         train_ratings, test_ratings = split_train_test(ratings, train_fraction=0.5)

    #         for t in ["answers + ratings", "ratings only", "answers only"]:
    #             if t == "answers + ratings":
    #                 include_weights = True
    #                 include_crowd_answers = True
    #             elif t == "ratings only":
    #                 include_weights = True
    #                 include_crowd_answers = False
    #             else:
    #                 include_crowd_answers = True
    #                 include_weights = False

    #             train_features, train_labels = create_dataset(train_ratings, include_crowd_answers, include_weights)
    #             model = train_model(train_features, train_labels)
    #             accuracy, precision, _, _, _, _, _, _ = test_model(model, test_ratings, include_crowd_answers, include_weights)
    #             for k in accuracy.iterkeys():
    #                 if k not in accuracies:
    #                     accuracies[k] = []
    #                     precisions[k] = []
    #                 accuracies[k].append(accuracy[k])
    #                 precisions[k].append(precision[k])
    #             thresholds.append(10 - THRESHOLD - 1)
    #             typ.append(t)

    # data = pd.DataFrame({"Number of workers": thresholds,
    #                      "avg-score": accuracies["Reranking model = "],
    #                      "model": typ})

    # sns.set(font_scale=2)
    # g = sns.factorplot(x="Number of workers", y="avg-score", hue="model", data=data, legend=False)
    # g.set(ylim=(2.3, 2.6))
    # sns.plt.legend(loc='upper left')
    # sns.plt.show()

    # data = pd.DataFrame({"Number of workers": thresholds,
    #                      "avg-prec": precisions["Reranking model = "],
    #                      "model": typ})

    # sns.set(font_scale=2)
    # g = sns.factorplot(x="Number of workers", y="avg-prec", hue="model", data=data, legend=False)
    # g.set(ylim=(2.3, 2.6))
    # sns.plt.legend(loc='upper left')
    # sns.plt.show()

    for i in xrange(1):
        train_ratings, test_ratings = split_train_test(ratings, train_fraction=0.5)
        train_features, train_labels = create_dataset(train_ratings, include_crowd_answers)
        model = train_model(train_features, train_labels)
        for category in [None, 'Dining Out', 'Politics & Government', 'News & Events', 'Home & Garden', 'Entertainment & Music', 'Education & Reference', 'Travel', 'Games & Recreation', 'Arts & Humanities', 'Pregnancy & Parenting', 'Sports', 'Family & Relationships', 'Society & Culture', 'Health', 'Pets', 'Beauty & Style', 'Business & Finance', 'Local Businesses', 'Computers & Internet', 'Cars & Transportation', 'Science & Mathematics']:

            accuracies = dict()
            precisions = dict()
            succ2 = dict()
            succ3 = dict()
            succ4 = dict()
            prec2 = dict()
            prec3 = dict()
            prec4 = dict()

            accuracy, precision, s2, s3, s4, p2, p3, p4, number_of_questions = test_model(model, test_ratings, include_crowd_answers, qid2title=qid2title, category=category)
            for k in accuracy.iterkeys():
                if k not in accuracies:
                    accuracies[k] = []
                    precisions[k] = []
                    succ2[k] = []
                    succ3[k] = []
                    succ4[k] = []
                    prec2[k] = []
                    prec3[k] = []
                    prec4[k] = []
                accuracies[k].append(accuracy[k])
                precisions[k].append(precision[k])
                succ2[k].append(s2[k])
                succ3[k].append(s3[k])
                succ4[k].append(s4[k])
                prec2[k].append(p2[k])
                prec3[k].append(p3[k])
                prec4[k].append(p4[k])

            if number_of_questions < 20: continue

            print "\n", category, number_of_questions
            for k in accuracies.iterkeys():
                print k
                print "Accuracy=", np.mean(accuracies[k]), "Precision=", np.mean(precisions[k]), "succ@2=", np.mean(succ2[k]), "succ@3=", np.mean(succ3[k]), "succ@4=", np.mean(succ4[k]), "prec@2=", np.mean(prec2[k]), "prec@3=", np.mean(prec3[k]), "prec@4=", np.mean(prec4[k])