import logging
import datetime
import os
import json
def create_dir_for_file(file_path):
    # type: (str) -> None
    """Creates any missing parent directories of this files path."""

    try:
        os.makedirs(os.path.dirname(file_path))
    except OSError:
        # be happy if someone already created the path
        pass

def create_logger(module_name, response_log_dir):
    """Creates a logger that will persist incomming queries and their results."""

    # Ensures different log files for different processes in multi worker mode
    if response_log_dir:
        # We need to generate a unique file name, even in multiprocess environments
        timestamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
        log_file_name = "{}_log-{}-{}.log".format(module_name, timestamp, os.getpid())
        response_logfile = os.path.join(response_log_dir, log_file_name)
        # Instantiate a standard python logger, which we are going to use to log requests
        logger = logging.getLogger('query-logger')
        logger.setLevel(logging.INFO)
        create_dir_for_file(response_logfile)
        ch = logging.FileHandler(response_logfile)
        ch.setFormatter(logging.Formatter('%(message)s'))
        logger.propagate = False  # Prevents queries getting logged with parent logger --> might log them to stdout
        logger.addHandler(ch)
        logging.info("Logging requests to '{}'.".format(response_logfile))
        return logger
    else:
        # If the user didn't provide a logging directory, we wont log!
        logging.info("Logging of requests is disabled. (No 'request_log' directory configured)")
        return None