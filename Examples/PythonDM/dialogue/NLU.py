

class NLU(object):

    def process(self, asr):
        if asr == "Hi there!":
            return "greeting"
        elif asr == "I want to see a movie":
            return "ask_for_recommendation"
        # ....
        else:
            return "user_intent"