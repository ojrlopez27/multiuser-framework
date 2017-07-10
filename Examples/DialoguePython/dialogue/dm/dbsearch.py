import requests
import json

data_template = {'device_id': 'yulun', 'query': None}

#data_template['filter'] = 'top'

query_template = {
        'function': 'rexplain', 

        'profile':{
              'likedMovies': [],  
              'likedEntities': [],  
              'dislikedMovies': [], 
              'dislikedEntities': []
        },

        'maxResults' : 1, 

        'environment': {
              'maxExplanations' : 2
        }
}

def set_filter(filter_str):
    data_template['filter'] = filter_str

def set_id(device_id):
    data_template['device_id'] = device_id

def frame2query(frame):
    #fill in likes
    for each in map(lambda x: x['id'], frame['frame']['genres']['like']):
        query_template['profile']['likedEntities'].append(each)
    for each in map(lambda x: x['id'], frame['frame']['actors']['like']):
        query_template['profile']['likedEntities'].append(each)
    for each in map(lambda x: x['id'], frame['frame']['directors']['like']):
        query_template['profile']['likedEntities'].append(each)
    #fill in dislikes
    for each in map(lambda x: x['id'], frame['frame']['genres']['dislike']):
        query_template['profile']['dislikedEntities'].append(each)
    for each in map(lambda x: x['id'], frame['frame']['actors']['dislike']):
        query_template['profile']['dislikedEntities'].append(each)
    for each in map(lambda x: x['id'], frame['frame']['directors']['dislike']):
        query_template['profile']['dislikedEntities'].append(each)

    # fill in movies
    for each in frame['frame']['movies']['like']:
        query_template['profile']['likedMovies'].append(each)
    for each in frame['frame']['movies']['dislike']:
        query_template['profile']['dislikedMovies'].append(each)

    return query_template

def db_search(temp):

    query_string = json.dumps(temp)
    data_template['query'] = query_string

    r = requests.post(' http://34.198.191.37:8080/imdbnew/query.jsp', data = data_template)
    return eval(r.text)









