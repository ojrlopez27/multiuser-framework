import pandas as pd

from fuzzywuzzy import fuzz
from fuzzywuzzy import process
import os 
dir_path = os.path.dirname(os.path.realpath(__file__))

directors = pd.read_csv(dir_path + '/imdb_data/director2id.txt', header = None, sep='\t')
actors = pd.read_csv(dir_path + '/imdb_data/actor2id.txt', header = None, sep='\t')
genres = pd.read_csv(dir_path + '/imdb_data/genre2id.txt', header = None, sep='\t')

table = {
            'genres': genres,
            'directors': directors,
            'actors': actors
        }


def fuzzy_match(ent_type, entities):
    # add confidence, 1 means found match, 0.5 means guessing, otherwise delete entity
    ret_entities = []
    for i, each in enumerate(entities):
        each['entity'] = ent_type
        # if in hash table, directly add
        if each['value'].lower() in table[ent_type][0].values:
            each['value'] = each['value'].lower()
            each['confidence'] = 1
            each['id'] = table[ent_type][table[ent_type][0]==each['value']].values[0][1]
            ret_entities.append(each)
        else:
            # exact match unigram for genres, could be revised by fuzzy match later
            fuzzy_result = process.extractOne(each['value'].lower(), table[ent_type][0], scorer = fuzz.token_set_ratio)
            if fuzzy_result[1] >= 90:
                each['value'] = fuzzy_result[0]
                each['confidence'] = 0.5
                each['id'] = table[ent_type][table[ent_type][0]==each['value']].values[0][1]
                ret_entities.append(each)
            else:
                if each['entity'] == 'genres':
                    for tok in each['value'].lower().split(' '):
                        if tok in table['genres'][0].values:
                            each['value'] = table['genres'][table['genres'][0]==tok].values[0][1]
                            each['confidence'] = 0.5
                            each['id'] = table['genres'][table['genres'][0]==tok].values[0][1]
                            ret_entities.append(each)
                            break

    return ret_entities

def exhaustive_fuzzy_match(entities):
    # add confidence, 1 means found match, 0.5 means guessing, otherwise delete entity
    ret_entities = []
    
    for i, each in enumerate(entities):
        switch = True
        # first loop for exact match, fast
        for ent_type in ["genres", "directors", "actors"]:

            if each['value'].lower() in table[ent_type][0].values:
                each['value'] = each['value'].lower()
                each['confidence'] = 1
                each['entity'] = ent_type
                each['id'] = table[ent_type][table[ent_type][0]==each['value']].values[0][1]
                ret_entities.append(each)
                switch = False
                break
        
        if switch:
            # second loop for fuzzy match, slow
            for ent_type_2 in ["genres", "directors", "actors"]:
                # exact match unigram for genres, could be revised by fuzzy match later
                fuzzy_result = process.extractOne(each['value'].lower(), table[ent_type_2][0], scorer = fuzz.token_set_ratio)
                if fuzzy_result[1] >= 90:
                    each['value'] = fuzzy_result[0]
                    each['confidence'] = 0.5
                    each['entity'] = ent_type_2
                    each['id'] = table[ent_type_2][table[ent_type_2][0]==each['value']].values[0][1]
                    ret_entities.append(each)
                    break
                else:
                    if each['entity'] == 'genres':
                        found = False
                        for tok in each['value'].lower().split(' '):
                            if tok in table['genres'][0].values:
                                each['value'] = table['genres'][table['genres'][0]==tok].values[0][1]
                                each['confidence'] = 0.5
                                each['id'] = table['genres'][table['genres'][0]==tok].values[0][1]
                                ret_entities.append(each)
                                found = True
                                break
                        if found:
                            break

    return ret_entities

def exact_match(text, template = table['genres']):
    ret_val = []
    for each in text.lower().split(' '):
        if each in template[0].values:
            ret_val.append({
                    'entity': 'genres',
                    'id': template[template[0]==each].values[0][1],
                    'value': each,
                    'start': text.lower().find(each),
                    'end': text.lower().find(each) + len(each)
                }
                )
    return ret_val
                

