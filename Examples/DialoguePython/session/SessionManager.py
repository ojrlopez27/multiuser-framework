
import Constants
import threading
import json
from communication.ServerController import ServerController
from communication.Broker import Broker
from session.Session import Session

class SessionManager(threading.Thread):

    verbose = True
    server = None
    broker = None
    sessions = dict()

    def __init__(self, verbose=True):
        super().__init__()
        self.verbose = verbose
        self.server = ServerController("tcp://localhost:" + Constants.port, "session-manager", verbose)
        self.broker = Broker(verbose)
        self.broker.start()

    def run(self):
        while Constants.stop is not True:
            request = self.server.recv()
            if request is None:
                break  # Worker was interrupted
            self.process(request)
        self.destroy()

    def process(self, request):
        request_data = json.loads(str(request[2], "UTF-8"))
        session_id = request_data["sessionId"]
        if request_data["requestType"] == "REQUEST_CONNECT":
            self.create_session(session_id, request)
        elif request_data["requestType"] == "REQUEST_DISCONNECT":
            self.destroy_session(session_id, request)
        elif request_data["requestType"] == "REQUEST_CLOSE":
            self.close()

    def create_session(self, session_id, reply):
        print('create session: %s' % session_id)
        session = Session(session_id, self.verbose)
        self.sessions[session_id] = session
        session.start()
        reply[2] = bytes(str(reply[2], "UTF-8").replace("REQUEST_CONNECT", "SESSION_INITIATED"), "UTF-8")
        self.server.send(reply)
        
        # # yulun: test if greeting workds
        # time.sleep(10.0/1000.0)
        # g = {"frame": {"ask_stack": ["genres", "directors", "actors", "recommend"], "frame": {"directors": {"dislike": [], "like": []}, "genres": {"dislike": [], "like": []}, "movies": {"history": [], "dislike": [], "like": []}, "actors": {"dislike": [], "like": []}}, "universals": ["help", "start_over"]}, "action": "greeting", "entities": [], "recommendation": {"rexplanations": [{"explanations": [], "recommendation": ""}]}}
        # reply[2] = bytes(json.dumps(g), "UTF-8")
        # self.server.send(reply)

    def destroy_session(self, session_id, reply):
        print('destroy session: %s' % session_id)
        session = self.sessions.pop(session_id)
        session.destroy_session()
        reply[2] = bytes(str(reply[2], "UTF-8").replace("REQUEST_DISCONNECT", "SESSION_CLOSED"), "UTF-8")
        self.server.send(reply)

    def close(self):
        print('closing Session Manager...')
        Constants.stop = True

    def destroy(self):
        for k, v in self.sessions.items():
            v.destroy_session()
        self.sessions.clear()
        self.server.destroy()

