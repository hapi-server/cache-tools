from setuptools import setup, find_packages

install_requires = ["fastapi"]

setup(
    name='hapi-cache-server',
    version='0.0.1',
    author='Bob Weigel',
    author_email='rweigel@gmu.edu',
    packages=find_packages(),
    license='LICENSE.txt',
    description='Server for HAPI cache library.',
    long_description=open('README.md').read(),
    long_description_content_type='text/markdown',
    install_requires=install_requires
)
