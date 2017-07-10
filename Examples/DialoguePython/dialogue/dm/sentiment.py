from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer

analyzer = SentimentIntensityAnalyzer()

def sentiment_update(entities, context):
    # assuming prefix score is always preferred than suffix
    for each in entities:
        prefix = context[each['value']][0]
        suffix = context[each['value']][1]
        prefix_score = analyzer.polarity_scores(prefix)['compound']
        suffix_score = analyzer.polarity_scores(suffix)['compound']
        if prefix_score >= 0 and suffix_score >= 0:
            each['polarity'] = (prefix_score + suffix_score)/2
        elif prefix_score < 0 and suffix_score < 0:
            each['polarity'] = (prefix_score + suffix_score)/2
        elif prefix_score == 0:
            each['polarity'] = suffix_score
        elif suffix_score == 0 or prefix_score >= 0.3 or prefix_score <= -0.3:
            each['polarity'] = prefix_score
        elif abs(prefix_score) > abs(suffix_score):
            each['polarity'] = prefix_score
        else:
            each['polarity'] = (2 * prefix_score + suffix_score)/2

    return entities

def entity2context(text, entities):
    ret_dic = {}
    last_val = None
    last_end = 0
    for each in entities:
        ret_dic[each['value']] = [text[last_end:each['start']]]
        if last_end == 0:
            last_end = each['end']
            last_val = each['value']
            continue

        ret_dic[last_val].append(text[last_end:each['start']])
        last_end = each['end']
        last_val = each['value']

    ret_dic[last_val].append(text[last_end:len(text)])

    return ret_dic




