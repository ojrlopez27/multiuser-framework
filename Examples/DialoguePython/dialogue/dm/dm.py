import json
import copy
from dialogue.dm.fuzzy import *
from dialogue.dm.sentiment import *
from dialogue.dm.dbsearch import *
import pandas as pd

# # for testing
# from fuzzy import *
# from sentiment import *
# from dbsearch import *

class DialogManager():
    # TODO: modulize this huge chunk

    def __init__(self, filename):

        self.filename = filename

        self.error_handling = None
        self.error_cache = []

        self.number_of_rejects = 0
        self.number_of_updates = 0
        self.last_system_intent = None
        #self.greeted = False

        f = open(self.filename)
        s = f.read()
        f.close()
        self.frame = json.loads(s)

        print("Dialog manager start! current in the start state!")

    def print_state(self):
        print(json.dumps(self.frame, indent=4, sort_keys=True))

    # def request_recommend(self):
    #     print("db search!")
    #     return {
    #                 "value": "fake_movie_name_" + str(self.number_of_updates),
    #                 "polarity": 0,
    #                 "entity": "movie"
    #             }

    # def fake_rose_system(self, data=None):
    #     recommendations = ["Forrest Gump (1994)", "Women, The (1939)", "August (1996)"]
    #     explanations = ["stomcruise", "smorganfreeman", "santhonyhopkins"]

    #     template = { "rexplanations":
    #                     [
    #                         {
    #                             "recommendation": recommendations[self.number_of_updates%3],
    #                             "explanations": [explanations[self.number_of_updates%3]]
    #                         }
    #                     ]
    #                 }
    #     return template

    def none_recommendation(self):
        return { "rexplanations":
                        [
                            {
                                "recommendation": '',
                                "explanations": []
                            }
                        ]
                    }

    def true_rose_system(self):
        query = frame2query(self.frame)
        return db_search(query)

    def universal(self, intent):
        if intent == 'help':
            print("help is given!")
        if intent == 'start_over':
            self.__init__(self.filename)
            self.frame['ask_stack'].remove('start')
            intent = 'greeting'
            print("starting over!")
        return intent

    def clear_error(self):
        self.error_handling = None
        self.error_cache = []

    def slot_filling(self, slot, values):
        for entity in values:
            if entity['polarity'] >= 0:
                self.frame['frame'][slot]['like'].append(entity)
            else:
                self.frame['frame'][slot]['dislike'].append(entity)


    def belief_update(self, slot, values):
        for entity in values:
            if entity['polarity'] >= 0:
                if entity['value'] in map(lambda x: x['value'], self.frame['frame'][slot]['dislike']):
                    self.frame['frame'][slot]['dislike'] = filter(lambda x: x['value'] != entity['value'], self.frame['frame'][slot]['dislike'])
                if entity['value'] in map(lambda x: x['value'], self.frame['frame'][slot]['like']):
                    continue
                self.frame['frame'][slot]['like'].append(entity)
            else:
                if entity['value'] in map(lambda x: x['value'], self.frame['frame'][slot]['like']):
                    self.frame['frame'][slot]['like'] = filter(lambda x: x['value'] != entity['value'], self.frame['frame'][slot]['like'])
                if entity['value'] in map(lambda x: x['value'], self.frame['frame'][slot]['dislike']):
                    continue
                self.frame['frame'][slot]['dislike'].append(entity)   


    def to_nlg(self, action):
        # the information sent to nlg, including action and frame
        print("not yet implemented")

    def transit(self, intent, entities, c, text):

        # if there are entities, check if they are fuzzily in the db:
        if len(entities) > 0:
            if self.frame['ask_stack'][0] in ["genres", "directors", "actors"]:
                entities = fuzzy_match(self.frame['ask_stack'][0], entities)
            else:
                entities = exhaustive_fuzzy_match(entities)
        else:
            #check if there is genres in the text
            entities = exact_match(text)

        if len(entities) > 0:
            # get entities context and update sentiment
            context = entity2context(text, entities)
            entities = sentiment_update(entities, context)


        if intent in self.frame['universals']:
            action = self.universal(intent)
            return action, [], self.none_recommendation()

        if intent == 'goodbye':
            action = 'goodbye'
            self.frame['ask_stack'] = []
            return action, [], self.none_recommendation()

        # start state
        if 'start' in self.frame['ask_stack']:
            # commented out because of system initiative
            #action = 'greeting'
            #self.greeted = True
            self.frame['ask_stack'].remove('start')
            #return action, [], self.none_recommendation()

        if self.number_of_updates == 0 and len(self.frame['ask_stack']) > 0:
            action = 'ask_genres'
            self.number_of_updates += 1
            return action, [], self.none_recommendation()

        # first state
        if 'genres' in self.frame['ask_stack']:
            # error handling state
            if self.error_handling == 'genres':
                if intent == 'no':
                    action = 'ask_genres'
                    self.number_of_updates += 1
                    self.clear_error()
                    return action, [], self.none_recommendation()
                elif intent == 'yes':
                    action = 'ask_directors'
                    self.number_of_updates += 1

                    self.slot_filling('genres', self.error_cache)

                    self.frame['ask_stack'].remove('genres')
                    self.clear_error()
                    return action, self.error_cache, self.none_recommendation()
                elif intent == 'inform' and c == 'high':
                    self.clear_error()
                    # low medium high???
                    self.belief_update('genres', entities)
                    action = 'ask_directors'
                    return action, entities, self.none_recommendation()

                action = 'explicit_confirm'
                self.number_of_updates += 1
                return action, self.error_cache, self.none_recommendation()
                    
            if intent == 'inform':
                if len(entities) == 0 or c == 'low':
                    action = 'ask_repeat'
                    self.number_of_updates += 1
                    return action, [], self.none_recommendation()
                if c == 'medium':
                    action = 'explicit_confirm'
                    self.number_of_updates += 1
                    self.error_handling = 'genres'
                    self.error_cache = entities
                    return action, self.error_cache, self.none_recommendation()
                if c == 'high':
                    action = 'ask_directors'
                    self.number_of_updates += 1

                    self.slot_filling('genres', entities)

                    self.frame['ask_stack'].remove('genres')
                    return action, entities, self.none_recommendation()


            elif intent == 'no':
                action = 'ask_directors'
                self.number_of_updates += 1
                self.frame['ask_stack'].remove('genres')
                return action, [], self.none_recommendation()


            action = 'ask_genres'
            self.number_of_updates += 1
            
            return action, [], self.none_recommendation()
                    
        # second state
        if 'directors' in self.frame['ask_stack']:
            # error handling state
            if self.error_handling == 'directors':
                if intent == 'no':
                    action = 'ask_directors'
                    self.number_of_updates += 1
                    self.clear_error()
                    return action, [], self.none_recommendation()
                elif intent == 'yes':
                    action = 'ask_actors'
                    self.number_of_updates += 1
                    self.slot_filling('directors', self.error_cache)
                    self.frame['ask_stack'].remove('directors')
                    self.clear_error()
                    return action, self.error_cache, self.none_recommendation()
                elif intent == 'inform' and c == 'high':
                    self.clear_error()
                    self.belief_update('directors', entities)
                    action = 'ask_actors'
                    return action, entities, self.none_recommendation()

                action = 'explicit_confirm'
                self.number_of_updates += 1
                return action, self.error_cache, self.none_recommendation()
                    
            if intent == 'inform':
                if len(entities) == 0 or c == 'low':
                    action = 'ask_repeat'
                    self.number_of_updates += 1
                    return action, [], self.none_recommendation()
                if c == 'medium':
                    action = 'explicit_confirm'
                    self.number_of_updates += 1
                    self.error_handling = 'directors'
                    self.error_cache = entities
                    return action, self.error_cache, self.none_recommendation()
                if c == 'high':
                    action = 'ask_actors'
                    self.number_of_updates += 1
                    self.slot_filling('directors', entities)
                    self.frame['ask_stack'].remove('directors')

                    return action, entities, self.none_recommendation()


            elif intent == 'no':
                action = 'ask_actors'
                self.number_of_updates += 1
                self.frame['ask_stack'].remove('directors')
                return action, [], self.none_recommendation()

                
            action = 'ask_directors'
            self.number_of_updates += 1

            return action, [], self.none_recommendation()

        # third state
        if 'actors' in self.frame['ask_stack']:
            # error handling state
            if self.error_handling == 'actors':
                if intent == 'no':
                    action = 'ask_actors'
                    self.number_of_updates += 1
                    self.clear_error()
                    return action, [], self.none_recommendation()
                if intent == 'yes':
                    action = 'recommend'
                    self.number_of_updates += 1
                    
                    self.slot_filling('actors', self.error_cache)

                    self.frame['ask_stack'].remove('actors')
                    self.clear_error()
                    
                    
                    recommendation = self.true_rose_system()
                    self.frame['frame']['movies']['like'].append(recommendation["rexplanations"][0]["recommendation"])
                    return action, [], recommendation

                elif intent == 'inform' and c == 'high':
                    self.clear_error()
                    self.belief_update('actors', entities)
                    action = 'recommend'

                    recommendation = self.true_rose_system()
                    self.frame['frame']['movies']['like'].append(recommendation["rexplanations"][0]["recommendation"])
                    return action, entities, recommendation

                action = 'explicit_confirm'
                self.number_of_updates += 1
                return action, self.error_cache, self.none_recommendation()
                    
            if intent == 'inform':
                if len(entities) == 0 or c == 'low':
                    action = 'ask_repeat'
                    self.number_of_updates += 1
                    return action, [], self.none_recommendation()
                if c == 'medium':
                    action = 'explicit_confirm'
                    self.number_of_updates += 1
                    self.error_handling = 'actors'
                    self.error_cache = entities
                    return action, self.error_cache, self.none_recommendation()
                if c == 'high':
                    action = 'recommend'
                    self.number_of_updates += 1
                    self.slot_filling('actors', entities)
                    self.frame['ask_stack'].remove('actors')
                    

                    recommendation = self.true_rose_system()
                    self.frame['frame']['movies']['like'].append(recommendation["rexplanations"][0]["recommendation"])
                    return action, [], recommendation

            elif intent == 'no':
                action = 'recommend'
                self.number_of_updates += 1
                
                self.frame['ask_stack'].remove('actors')

                recommendation = self.true_rose_system()
                self.frame['frame']['movies']['like'].append(recommendation["rexplanations"][0]["recommendation"])
                return action, [], recommendation

            action = 'ask_actors'
            self.number_of_updates += 1
            return action, [], self.none_recommendation()
                
        # second to last state
        if 'recommend' in self.frame['ask_stack']:

            if self.error_handling == 'movies':
                if intent == 'no':
                    action = 'recommend'
                    self.number_of_updates += 1
                    self.clear_error()

                    recommendation = self.true_rose_system()
                    self.frame['frame']['movies']['like'].append(recommendation["rexplanations"][0]["recommendation"])
                
                    return action, [], recommendation

                if intent == 'yes':
                    action = 'recommend'
                    self.number_of_updates += 1

                    # update to histoy before belief update
                    history = self.frame['frame']['movies']['like'].pop()
                    self.frame['frame']['movies']['dislike'].append(history)

                    for each in self.error_cache:
                        self.belief_update(each['entity'], [each])

                    self.clear_error()
                    
                    recommendation = self.true_rose_system()
                    self.frame['frame']['movies']['like'].append(recommendation["rexplanations"][0]["recommendation"])
                    
                    return action, [], recommendation

                elif intent == 'inform' and c == 'high':
                    action = 'recommend'

                    # update to histoy before belief update
                    history = self.frame['frame']['movies']['like'].pop()
                    self.frame['frame']['movies']['dislike'].append(history)

                    self.clear_error()
                    for each in entities:
                        self.belief_update(each['entity'], [each])

                    recommendation = self.true_rose_system()
                    self.frame['frame']['movies']['like'].append(recommendation["rexplanations"][0]["recommendation"])
                    
                    return action, [], recommendation

                action = 'explicit_confirm'
                self.number_of_updates += 1
                return action, self.error_cache, self.none_recommendation()


            if intent == 'no':

                history = self.frame['frame']['movies']['like'].pop()
                self.frame['frame']['movies']['dislike'].append(history)

                action = 'recommend'
                self.number_of_updates += 1

                recommendation = self.true_rose_system()
                self.frame['frame']['movies']['like'].append(recommendation["rexplanations"][0]["recommendation"])
                
                return action, [], recommendation


            if intent == 'inform':

                if len(entities) == 0 or c == 'low':
                    action = 'ask_repeat'
                    self.number_of_updates += 1
                    return action, [], self.none_recommendation()
                if c == 'medium':
                    action = 'explicit_confirm'
                    self.number_of_updates += 1
                    self.error_handling = 'movies'
                    self.error_cache = entities
                    return action, self.error_cache, self.none_recommendation()
                if c == 'high':
                    action = 'recommend'
                    self.number_of_updates += 1

                    history = self.frame['frame']['movies']['like'].pop()
                    self.frame['frame']['movies']['dislike'].append(history)

                    for each in entities:
                        self.belief_update(each['entity'], [each])
                    
                    recommendation = self.true_rose_system()
                    self.frame['frame']['movies']['like'].append(recommendation["rexplanations"][0]["recommendation"])
                
                    return action, [], recommendation


            if intent == 'request_more':
                self.frame['frame']['movies']['history'].append(self.frame['frame']['movies']['like'][-1])
                action = 'recommend'
                self.number_of_updates += 1
                
                recommendation = self.true_rose_system()
                self.frame['frame']['movies']['like'].append(recommendation["rexplanations"][0]["recommendation"])
                # TODO
                return action, [], recommendation

            if intent == 'thank' or intent == 'yes':
                self.frame['ask_stack'].remove('recommend')
                action = 'goodbye'
                self.number_of_updates += 1
                return action, [], self.none_recommendation()

            # TODO, what is going on when users have other intents (actually none of this will happen)
            action = 'recommend'
            self.number_of_updates += 1
            self.frame['frame']['movies']['history'].append(self.frame['frame']['movies']['like'][-1])
            
            recommendation = self.true_rose_system()
            self.frame['frame']['movies']['like'].append(recommendation["rexplanations"][0]["recommendation"])

            return action, [], recommendation
                 
        action = 'help'
        self.number_of_updates += 1
        return action, [], self.none_recommendation()
        
        
if __name__ == '__main__':

    dm = DialogManager('frame.json')

    while True:

        raw_val = raw_input('Input as INTENT, ENTITIES, CONFIDENCE  : ')
        val = raw_val.split(', ')
        action = dm.transit(val[0], eval(val[1]), val[2])
        print("System intent: " + action)
        dm.print_state()


        
    
        
        
        
        
        