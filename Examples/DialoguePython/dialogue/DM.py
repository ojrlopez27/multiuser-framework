from __future__ import unicode_literals
from __future__ import print_function
from __future__ import division
from __future__ import absolute_import
import argparse
import logging
import os
from functools import wraps
import time
import json
import io

from dialogue.dm.dm import DialogManager


class DM(object):

    def __init__(self, f = os.path.dirname(os.path.realpath(__file__)) + "/dm/frame.json"):
        self.filename = f
        self.user_count = 5
        self.dm = [DialogManager(self.filename) for i in range(self.user_count)]
        self.users = []

    def extent_users(count):
        self.user_count += count
        self.dm += [DialogManager(self.filename) for i in range(count)]

    def process(self, user_id, nlu_out):

        if user_id not in self.users:
            self.users.append(user_id)

            if len(self.users) > self.user_count:
                self.extent_users(5)

        user_dm = self.dm[self.users.index(user_id)]
        action, entities, recommendation = user_dm.transit(nlu_out['intent']['name'], nlu_out['entities'], 'high', nlu_out['text'])

        retval = {
                    'action': action, 
                    'entities': entities, 
                    'frame': user_dm.frame,
                    "recommendation": recommendation
            }
        return retval

if __name__ == '__main__':
    dm = DM()
    print(dm.process('uid_1', {'intent':{'name':'greeting'}, 'entities':[]}))
    print(dm.process('uid_1', {'intent':{'name':'greeting'}, 'entities':[]}))
    print(dm.process('uid_2', {'intent':{'name':'greeting'}, 'entities':[]}))














