
import time
import Constants
from session.SessionManager import SessionManager

def main():
    verbose = Constants.verbose  # '-v' in sys.argv
    session_manager = SessionManager(verbose)
    session_manager.start()

    while Constants.stop is not True:
        time.sleep(10.0/1000.0) # 10 ms

if __name__ == '__main__':
    main()