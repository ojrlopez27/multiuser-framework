
class DM(object):

    state_machine = None

    def __init__(self):
        self.state_machine = dict()


    def process(self, user_intent):
        # ....
        # update your model:
        # self.state_machine['something'] = "something"
        # ...

        if user_intent == "greeting":
            return "nice_to_see_you"
        elif user_intent == "ask_for_recommendation":
            return "give_recommendation"
        # ...
        else:
            return "system_intent"
