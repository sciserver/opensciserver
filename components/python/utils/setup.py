#!/usr/bin/env python

import os
from setuptools import setup, find_packages


build_tag = os.getenv('BUILD_VERSION_TAG')


setup(
    name='sciserver-utils',
    version='0.1' + (f'+{build_tag}' if build_tag else ''),
    description='SciServer Python Utilities for Application Development',
    author='Arik Mitschang',
    author_email='arik@jhu.edu',
    packages=find_packages('.'),
    install_requires=[
        'requests',
        'pika',
    ],
    extras_require={
        'fastapi': ['fastapi'],
    },
    python_requires='>=3.8',
    test_suite='nose.collector',
    tests_require=[
        'nose',
    ]
)
