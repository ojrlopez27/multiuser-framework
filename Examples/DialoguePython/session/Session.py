import Constants

from communication.ServerController import ServerController
from dialogue.DM import DM
from dialogue.NLU import NLU
import threading
import json

class Session(threading.Thread):
    verbose = True
    server = None
    stop = False
    session_id = None
    dm = None           # Dialogue Manager
    nlu = None          # NLU (should it be stateless? -- singleton?)

    def __init__(self, session_id, verbose=True):
        super().__init__()
        self.session_id = session_id
        self.verbose = verbose
        self.server = ServerController("tcp://localhost:" + Constants.port, session_id, verbose)
        self.dm = DM()
        self.nlu = NLU()


    def run(self):
        while self.stop is not True:
            request = self.server.recv()
            if request is None:
                break  # Worker was interrupted
            self.process(request)
        self.server.destroy()

    def process(self, request):
        request_data = json.loads(str(request[2], "UTF-8"))
        if 'ASRinput' in request_data:
            asr = request_data['ASRinput']
            user_intent = self.nlu.process(asr)
            system_intent = self.dm.process(user_intent)
            # you have to build a proper json here:
            request[2] = bytes("{systemIntent: '" + system_intent + "', userIntent: {userIntent: '"
                               + user_intent+ "'}}", "UTF-8")
            self.server.send(request)


    def destroy_session(self):
        print('destroy session')
        self.stop = True



