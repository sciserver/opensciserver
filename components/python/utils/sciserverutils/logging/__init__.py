import logging
import os
from logging.config import dictConfig


DEFAULT_FMT = '%(asctime)s [%(name)s] %(levelname)s: %(message)s'
DEFAULT_DATEFMT = '%Y-%m-%dT%H:%M:%S'
NOTIFIER_LOGGER = 'notify'
DEFAULT_CONFIG = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'outfmt': {
            'format': DEFAULT_FMT,
            'datefmt': DEFAULT_DATEFMT,
        }
    },
    'handlers': {
        'sciserverrmq': {
            'class': f'{__package__}.handlers.SciServerRmqHandlerFromEnviron'
        },
        'slacknotify': {
            'class': f'{__package__}.handlers.SlackWebhookHandlerFromEnviron'
        },
        'stderr': {
            'class': 'logging.StreamHandler',
            'formatter': 'outfmt',
        },
    },
    'root': {
        'handlers': ['sciserverrmq', 'stderr'],
        'level': 'INFO',
    },
    'loggers': {
        NOTIFIER_LOGGER: {
            'handlers': ['slacknotify'],
            'level': 'DEBUG',
        }
    }
}
DUMMY_CONFIG = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'outfmt': {
            'format': DEFAULT_FMT,
            'datefmt': DEFAULT_DATEFMT,
        }
    },
    'handlers': {
        'stderr': {
            'class': 'logging.StreamHandler',
            'formatter': 'outfmt',
        },
    },
    'root': {
        'handlers': ['stderr'],
        'level': 'DEBUG',
    },

}
debug = logging.debug
info = logging.info
warning = logging.warning
error = logging.error
getLogger = logging.getLogger
notify = logging.getLogger(NOTIFIER_LOGGER)


def configure():
    if os.getenv('SCISERVER_LOGGING_USE_DUMMY'):
        dictConfig(DUMMY_CONFIG)
    else:
        dictConfig(DEFAULT_CONFIG)
