from dialogue.DM import DM
from dialogue.NLU import NLU
import json

if __name__ == '__main__':
    nlu = NLU('dialogue/config.json')
    dm = DM('dialogue/dm/frame.json')
    asr = "Could you recommend a movie"
    nlu_out = nlu.process(asr)
    dm_out = dm.process('uid_1', nlu_out)
    #print(dm_out)
    o = bytes(json.dumps(dm_out), "UTF-8")
    print(o)