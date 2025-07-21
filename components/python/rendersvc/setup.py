#!/usr/bin/env python

import os
from setuptools import setup, find_packages


build_tag = os.getenv('BUILD_VERSION_TAG')


setup(
    name='sciserver-rendersvc',
    version='0.1' + (f'+{build_tag}' if build_tag else ''),
    description='SciServer Python Utilities for Application Development',
    author='Arik Mitschang',
    author_email='arik@jhu.edu',
    package_dir = {"": "src"},
    install_requires=[
        'requests',
        'sciserver-utils',
        'nbconvert>=6',
        'nbformat>=5',
        'fastapi',
    ],
    python_requires='>=3.8'
)
