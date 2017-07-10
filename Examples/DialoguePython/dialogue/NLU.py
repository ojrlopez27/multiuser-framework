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

from dialogue.nlu.config import NLUConfig
from dialogue.nlu.data_router import DataRouter, InvalidModelError


class NLU(object):
    def __init__(self, f = os.path.dirname(os.path.realpath(__file__)) + "/config.json"):

        self.config = NLUConfig(f, os.environ, {'config': 'config.json'})
        self.data_router = DataRouter(self.config)

    def process(self, asr):
        data = {'text': asr, 'model': 'default'}
        return self.data_router.parse(data)


if __name__ == '__main__':

    nlu = NLU()
    print(nlu.process('I love action movie'))

    # text = 'I love action movie'
    # data = {'text': text, 'model': 'default'}
    # print(data_router.parse(data))

