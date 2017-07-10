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

from nlu.config import NLUConfig
from nlu.data_router import DataRouter, InvalidModelError


def create_arg_parser():
    parser = argparse.ArgumentParser(description='parse incoming text')
    parser.add_argument('-c', '--config',
                        help="config file, all the command line options can also be passed via a (json-formatted) " +
                             "config file. NB command line args take precedence")
    parser.add_argument('-d', '--server_model_dirs',
                        help='directory containing model to for parser to use')
    parser.add_argument('-e', '--emulate', choices=['wit', 'luis', 'api'],
                        help='which service to emulate (default: None i.e. use simple built in format)')
    parser.add_argument('-l', '--language', choices=['de', 'en'], help="model and data language")
    parser.add_argument('-m', '--mitie_file',
                        help='file with mitie total_word_feature_extractor')
    parser.add_argument('-p', '--path', help="path where model files will be saved")
    parser.add_argument('-P', '--port', type=int, help='port on which to run server')
    parser.add_argument('-t', '--token',
                        help="auth token. If set, reject requests which don't provide this token as a query parameter")
    parser.add_argument('-w', '--write', help='file where logs will be saved')

    return parser


def requires_auth(f):
    """Wraps a request handler with token authentication."""

    @wraps(f)
    def decorated(*args, **kwargs):
        token = request.args.get('token', '')
        if current_app.data_router.token is None or token == current_app.data_router.token:
            return f(*args, **kwargs)
        return "unauthorized", 401

    return decorated


def create_app(config):
    nlu_app = Flask(__name__)

    @nlu_app.route("/parse", methods=['GET', 'POST'])
    @requires_auth
    def parse_get():
        if request.method == 'GET':
            request_params = request.args
        else:
            request_params = request.get_json(force=True)
        if 'q' not in request_params:
            return jsonify(error="Invalid parse parameter specified"), 404
        else:
            try:
                data = current_app.data_router.extract(request_params)
                response = current_app.data_router.parse(data)
                return jsonify(response)
            except InvalidModelError as e:
                return jsonify({"error": e.message}), 404

    @nlu_app.route("/status", methods=['GET'])
    @requires_auth
    def status():
        return jsonify(current_app.data_router.get_status())

    @nlu_app.route("/", methods=['GET'])
    @requires_auth
    def hello():
        return "hello"

    @nlu_app.route("/train", methods=['POST'])
    @requires_auth
    def train():
        data_string = request.get_data(as_text=True)
        print("yulun! " + data_string)
        current_app.data_router.start_train_process(data_string)

        return jsonify(info="training started. Current pids: {}".format(current_app.data_router.train_procs))


    @nlu_app.route("/save", methods=['POST'])
    @requires_auth
    def save():
        # wait for json saved...
        time.sleep(1)
        
        with io.open('./data/data.json', encoding="utf-8-sig") as f:
            data = json.loads(f.read())
        common = data['nlu_data'].get("common_examples", list())
        intents_new = list(set([ each.get("intent") for each in common ]))

        f = open('./data/intents.json', 'w')
        f.write(str(json.dumps({
            "intents" : intents_new,
            })))
        f.close()

        return "New intent data saved!"




    logging.basicConfig(filename=config['log_file'], level=config['log_level'])
    logging.captureWarnings(True)
    logging.info("Configuration: " + config.view())

    logging.debug("Creating a new data router")
    nlu_app.data_router = DataRouter(config)
    return nlu_app





# arg_parser = create_arg_parser()
# cmdline_args = {key: val for key, val in list(vars(arg_parser.parse_args()).items()) if val is not None}
# nlu_config = NLUConfig(cmdline_args.get("config"), os.environ, cmdline_args)

# @werkzeug.serving.run_with_reloader



if __name__ == '__main__':
    # Running as standalone python application
    arg_parser = create_arg_parser()
    cmdline_args = {key: val for key, val in list(vars(arg_parser.parse_args()).items()) if val is not None}
    nlu_config = NLUConfig(cmdline_args.get("config"), os.environ, cmdline_args)

    @werkzeug.serving.run_with_reloader
    def run_server():
        app = create_app(nlu_config)
        app.debug = True
        CORS(app)
        ws = WSGIServer(('0.0.0.0', nlu_config['port']), app)
        ws.serve_forever()

    run_server()
    # app = WSGIServer(('0.0.0.0', nlu_config['port']), create_app(nlu_config))
    # logging.info('Started http server on port %s' % nlu_config['port'])
    # app.serve_forever()











































