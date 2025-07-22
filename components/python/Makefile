all: rendersvc

utils.test:
	cd utils && tox

utils: utils.test
	cd utils && python -m build

rendersvc.test: utils
	cp utils/dist/*.whl rendersvc/
	cd rendersvc && tox

rendersvc: rendersvc.test utils
	cp utils/dist/*.whl rendersvc/
	cd rendersvc && python -m build

