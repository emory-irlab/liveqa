
import csv, random, sqlite3

from sys import argv

from scipy import stats
from sklearn.base import TransformerMixin, BaseEstimator
from sklearn.decomposition import LatentDirichletAllocation, TruncatedSVD
from sklearn.ensemble import GradientBoostingRegressor, RandomForestRegressor
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.grid_search import GridSearchCV
from sklearn.linear_model import SGDRegressor
from sklearn.pipeline import Pipeline, FeatureUnion


# BASE_PATH = "/home/dsavenk/data/liveqa/liveqa_16/run_logs/"
BASE_PATH = "/home/dsavenk/Projects/octiron/data/liveqa/liveqa_16/run_logs/"

def read_ratings(rating_file):
    question_ratings = dict()
    with open(rating_file, 'r') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            qid = row['Input.qid']
            if qid not in question_ratings:
                question_ratings[qid] = dict()
            for i in range(1, 10):
                if not row['Input.answer_' + str(i) + '_id']:
                    continue

                id = int(row['Input.answer_' + str(i) + '_id'])
                source = row['Input.answer_' + str(i) + '_source']
                rating = int(row['Answer.rating_' + str(i)])
                useful = row['Answer.useful_' + str(i)]
                if id not in question_ratings[qid]:
                    question_ratings[qid][id] = []
                question_ratings[qid][id].append((source, rating, useful))
    return question_ratings


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


def get_question_answers(qid, db_paths=(BASE_PATH + "crowd.db", BASE_PATH + "crowd_2.db")):
    for db in db_paths:
        conn = sqlite3.connect(db)
        c = conn.cursor()
        res = c.execute("SELECT ID, ANSWER, RANK, ANSWER_TYPE FROM ANSWERS WHERE QID = '%s'" % ('YA:' + qid)).fetchall()
        if res:
            conn.close()
            return res
        conn.close()
    return []


def get_answer_ratings(aid, db_paths=(BASE_PATH + "crowd.db", BASE_PATH + "crowd_2.db")):
    for db in db_paths:
        conn = sqlite3.connect(db)
        c = conn.cursor()
        res = c.execute("SELECT WORKER, RATING FROM RATINGS WHERE AID = %d" % aid).fetchall()
        if res:
            conn.close()
            return res
        conn.close()
    return []


def generate_features(qid, answers):
    features = []
    labels = []
    question = get_question(qid)
    answers_data = dict([(answer[0], answer[1:]) for answer in get_question_answers(qid)])
    for aid, ratings in answers.iteritems():
        if aid == -1:
            continue
        worker_ratings = [r[1] for r in get_answer_ratings(aid)]

        labels.append(1.0 * sum([rating[1] for rating in ratings]) / len(ratings))
        features.append([
            answers_data[aid][1],  # rank
            1.0 * sum(worker_ratings) / len(worker_ratings) if worker_ratings else 0.0,  # avg worker rating
            min(worker_ratings) if worker_ratings else 0.0,  # min worker rating
            max(worker_ratings) if worker_ratings else 0.0,  # max worker rating
            sorted(worker_ratings)[len(worker_ratings) / 2] if worker_ratings else 0.0,  # median worker rating
            0 if worker_ratings else 1,  # no worker ratings
            len(worker_ratings),         # number of worker ratings
            len(answers_data[aid][0]),   # answer length
            1.0 if answers_data[aid][2] == 0 else 0.0,   # source = 0
            1.0 if answers_data[aid][2] == 1 else 0.0,   # source = 1
            1.0 if answers_data[aid][2] == 2 else 0.0,   # source = 2
            1.0 if answers_data[aid][2] == 3 else 0.0,   # source = 3
            1.0 if answers_data[aid][2] == 4 else 0.0,   # source = 4
        ])
    return features, labels


def create_dataset(ratings):
    features = []
    labels = []
    for qid, answers in ratings.iteritems():
        f, l = generate_features(qid, answers)
        features.extend(f)
        labels.extend(l)
    return features, labels


def train_model(features, labels):
    regressor = GradientBoostingRegressor(n_estimators=100, max_depth=3, subsample=0.8, learning_rate=0.1)
    return regressor.fit(features, labels)


def test_model(model, test_ratings):
    original_scores = []
    scores = []
    heuristic_scores = []
    yahoo_scores = []
    crowdrating_only_scores = []
    crowdrating_only_heuristic_scores = []
    for qid, answers in test_ratings.iteritems():
        ya_answers = [ratings for aid, ratings in answers.iteritems() if aid == -1]
        yahoo_scores.append((1.0 * sum([r[1] for r in ya_answers[0]]) / len(ya_answers[0])) if ya_answers else 0.0)

        # Update answers to include only system answers.
        answers = dict([(aid, ratings) for aid, ratings in answers.iteritems() if aid != -1])

        if not answers:
            scores.append(0.0)
            original_scores.append(0.0)
            heuristic_scores.append(0.0)
            crowdrating_only_scores.append(0.0)
        else:
            features, labels = generate_features(qid, answers)
            predictions = model.predict(features)
            original = [ans for ans in sorted(enumerate(answers), key=lambda a:features[a[0]][0]) if features[ans[0]][0] != -1]
            heuristic = sorted(enumerate(answers), key=lambda a: features[a[0]][1], reverse=True)
            crowdrating_only = [ans for ans in sorted(enumerate(answers), key=lambda a:predictions[a[0]], reverse=True) if features[ans[0]][0] != -1]
            crowdrating_only_heuristic = [ans for ans in sorted(enumerate(answers), key=lambda a: features[a[0]][1], reverse=True) if features[ans[0]][0] != -1]
            reranking = sorted(enumerate(answers), key=lambda a: predictions[a[0]], reverse=True)
            scores.append(labels[reranking[0][0]])
            original_scores.append(labels[original[0][0]] if original else 0.0)
            crowdrating_only_scores.append(labels[crowdrating_only[0][0]] if crowdrating_only else 0.0)
            crowdrating_only_heuristic_scores.append(labels[crowdrating_only_heuristic[0][0]] if crowdrating_only_heuristic else 0.0)

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

    print "------------------ ACCURACY -----------------"
    print "Original ranking = ", 1.0 * sum(original_scores) / len(original_scores)
    print "My heuristic = ", 1.0 * sum(heuristic_scores) / len(heuristic_scores), stats.ttest_rel(original_scores, heuristic_scores)[1]
    print "Reranking model = ", 1.0 * sum(scores) / len(scores), stats.ttest_rel(original_scores, scores)[1]
    print "Reranking model (ratings only) = ", 1.0 * sum(crowdrating_only_scores) / len(crowdrating_only_scores), stats.ttest_rel(original_scores, crowdrating_only_scores)[1]
    print "Crowd ratings only = ", 1.0 * sum(crowdrating_only_heuristic_scores) / len(crowdrating_only_heuristic_scores), stats.ttest_rel(original_scores, crowdrating_only_heuristic_scores)[1]
    print "Yahoo! Answers = ", 1.0 * sum(yahoo_scores) / len(yahoo_scores), stats.ttest_rel(original_scores, yahoo_scores)[1]
    print "------------------ PRECISION -----------------"
    print "Original ranking = ", div_by_nonzero(sum(original_scores), original_scores)
    print "My heuristic = ", div_by_nonzero(sum(heuristic_scores), heuristic_scores)
    print "Reranking model = ", div_by_nonzero(sum(scores), scores)
    print "Reranking model (ratings only) = ", div_by_nonzero(sum(crowdrating_only_scores), crowdrating_only_scores)
    print "Crowd ratings only = ", div_by_nonzero(sum(crowdrating_only_heuristic_scores), crowdrating_only_heuristic_scores)
    print "Yahoo! Answers = ", div_by_nonzero(sum(yahoo_scores), yahoo_scores)


def div_by_nonzero(nom, list):
    denom = len([x for x in list if x != 0])
    return 1.0 * nom / denom if denom != 0 else 0.0


if __name__ == "__main__":
    random.seed(42)
    ratings = read_ratings(argv[1])
    train_ratings, test_ratings = split_train_test(ratings, train_fraction=0.5)
    train_features, train_labels = create_dataset(train_ratings)
    model = train_model(train_features, train_labels)
    test_model(model, test_ratings)