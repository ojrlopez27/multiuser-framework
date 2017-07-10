from setuptools import setup

tests_requires = [
    "pytest-pep8",
    "pytest-xdist",
    "pytest-services",
    "pytest-flask",
]

install_requires = [
    "requests",
    "pathlib",
    "cloudpickle",
    "boto3",
    "typing",
    "textblob",
    "future",
]

extras_requires = {
    'test': tests_requires,
    'http': ["flask", "gevent"],
    'spacy': ["sklearn", "scipy", "numpy"],
    'mitie': ["mitie", "numpy"],
}

setup(
    name='nlu',
    packages=[
        'nlu',
        'nlu.utils',
        'nlu.classifiers',
        'nlu.emulators',
        'nlu.extractors',
        'nlu.featurizers',
        'nlu.tokenizers',
    ],
    classifiers=[
        "Programming Language :: Python :: 2.7",
        "Programming Language :: Python :: 3.4",
        "Programming Language :: Python :: 3.5",
        "Programming Language :: Python :: 3.6"
    ],
    install_requires=install_requires,
    tests_require=tests_requires,
    extras_require=extras_requires,
)
