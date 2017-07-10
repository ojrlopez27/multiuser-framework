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

from flask import Flask
from flask import current_app
from flask import jsonify
from flask import request
from gevent.wsgi import WSGIServer
import werkzeug.serving
from flask_cors import CORS, cross_origin

from dm import DialogManager



def create_app(filename):
    dm_app = Flask(__name__)

    dm = DialogManager(filename)

    @dm_app.route("/track", methods=['POST'])
    def post():

        request_params = request.get_json(force=True)
        try:
            action, entities, recommendation = dm.transit(request_params['intent']['name'], request_params['entities'], request_params['intent']['confidence'], request_params['text'])
            return jsonify({"action": action, "entities": entities, "state": dm.frame, "recommendation":recommendation})
        except InvalidModelError as e:
            return jsonify({"error": e.message}), 404

    @dm_app.route("/", methods=['GET'])
    def hello():
        return "hello"

    return dm_app


if __name__ == '__main__':
    # Running as standalone python application
    

    @werkzeug.serving.run_with_reloader
    def run_server():
        app = create_app('frame.json')
        app.debug = True
        CORS(app)
        ws = WSGIServer(('0.0.0.0', 5555), app)
        ws.serve_forever()

    run_server()

