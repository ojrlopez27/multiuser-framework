import Constants

from communication.ServerController import ServerController
from dialogue.DM import DM
from dialogue.NLU import NLU
import threading
import json
import time

from log_utils import *
import datetime


class Session(threading.Thread):
    verbose = True
    server = None
    stop = False
    session_id = None
    dm = None  # Dialogue Manager
    nlu = None  # NLU (should it be stateless? -- singleton?)
    f = False
    hello_count = 0

    def __init__(self, session_id, verbose=True):
        super().__init__()
        self.session_id = session_id
        self.verbose = verbose
        self.server = ServerController("tcp://localhost:" + Constants.port, session_id, verbose)
        self.refresh()
        self.nlu = NLU()
        self.logger = create_logger('NLU_DM', 'logs', session_id)

    def refresh(self):
        if self.dm:
            self.dm.reset()
        else:
            self.dm = DM()

    def run(self):
        while self.stop is not True:
            request = self.server.recv()
            if request is None:
                break  # Worker was interrupted
            self.process(request)
        self.server.destroy()

    def process(self, request):
        # VIVIAN
        incremental = True  # incremental system

        #print(request)
        #request_data = json.loads(str(request[2], "UTF-8"))
        print(request)
        print("pos2: " + str(request[2], "UTF-8"))
        request_data = json.loads(str(request[2], "UTF-8"))
        print("payload: " + request_data['payload'])
        request_data = json.loads(request_data['payload'])['ASRinput']
        print("ASRinput: " + request_data)
        print("utterance: " + json.loads(request_data)['utterance'])
        if 'utterance' in request_data:

            asr = request_data['utterance']
            timestamp_asr = datetime.datetime.now().strftime('%Y%m%d-%H%M%S.%f')

            # dirty hacks for refreshing
            if asr == 'MSG_START_DM':
                # if self.hello_count % 2 == 0:
                self.refresh()
                asr = 'Hello'
                #    self.hello_count += 1
                # return
                # else:
                #    self.hello_count += 1
                #    return

            # print("Session: asr out: " + asr)

            if incremental is True and asr == 'MSG_QUERY':
                print("Session: request to get query")
                # thing (rename) is the recommendation
                thing = self.dm.get_movie_rec()
                # getresult_log = self.session_id + '::' + str(time.time()) + '::Getting result'
                # self.logger.info(getresult_log)
                # print(getresult_log)

                if thing is None:
                    dm_out = self.dm.underspecified_query(self.session_id)
                else:
                    dm_out = self.dm.specified_query(self.session_id)

                result_log = self.session_id + '::' + str(time.time()) + '::Result: ' + str(dm_out)
                self.logger.info(result_log)
                print(result_log)

                request[2] = bytes(json.dumps(dm_out), "UTF-8")

            else:
                nlu_out = self.nlu.process(asr)
                timestamp_nlu = datetime.datetime.now().strftime('%Y%m%d-%H%M%S.%f')
                # print("---- DM out:  " + str(dm_out))
                dm_out = self.dm.process(self.session_id, nlu_out, asr)
                timestamp_dm = datetime.datetime.now().strftime('%Y%m%d-%H%M%S.%f')

                self.dm.dm.print_state()
                # you have to build a proper json here:
                request[2] = bytes(json.dumps(dm_out), "UTF-8")

                # timestamped logging for individual modules
                asr_log = self.session_id + '::' + str(timestamp_asr) + '::ASR: ' + str(asr)
                nlu_log = self.session_id + '::' + str(timestamp_nlu) + '::NLU: ' + str(nlu_out)
                dm_log = self.session_id + '::' + str(timestamp_dm) + '::DM: ' + str(dm_out)
                # q_log = self.session_id + '::' + str(timestamp_query) + '::Query: ' + str(self.dm.dm.get_query())
                self.logger.info(asr_log)
                self.logger.info(nlu_log)
                self.logger.info(dm_log)
                # self.logger.info(q_log)
                print(asr_log)
                print(nlu_log)
                print(dm_log)
                # print(q_log)

            # VIVIAN
            # if not incremental, query for the movie now
            if incremental is not True:
                if dm_out['action'] == 'recommend':
                    self.dm.set_movie_rec(self.session_id)
                    dm_out = self.dm.specified_query(self.session_id)
                    request[2] = bytes(json.dumps(dm_out), "UTF-8")
            # end non-incremental system hack

            # print(request[2])
            print("Session: response from session ID (Session):" + self.session_id)
            self.server.send(request)
            print("Session: sent request to ServerController")

            # VIVIAN
            # incremental system
            if incremental is True:
                if dm_out['action'] == 'recommend' and asr != 'MSG_QUERY':
                    print("Session: querying")
                    self.dm.set_movie_rec(self.session_id)
            # end incremental system

            # exchange over (DM to start state)
            if dm_out['action'] == 'goodbye':
                self.refresh()
        elif 'frame' in request_data:
            self.logger.info("updating user frame: " + str(request_data['frame']))
            self.dm.update_frame(request_data['frame'])
        else:
            print("I got message that I could not understand: " + str(request_data))

    def destroy_session(self):
        print('destroy session')
        self.stop = True


self.server.destroy()