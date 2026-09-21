Integration tests for sciserver.

## About

These are meant to run via Jenkins and be triggered by automatic
updates to the repo-tracking installations.

The tests have some ordering requirements and maintain some state via
local files, in short, test should run in order

1. **test_preflight**
1. **test_register**
1. *other tests**
1. **test_logout**

In particular, the **test_register** sets up a random user to use for
the test session for those test requiring authentication, while the
**test_logout** test dismantles the session and checks authorization
no longer valid.

## Caveats

* The registration tests do not support email verification in
  registration, if that is required they will fail! No test presently
  for logging in from supplied credentials

* Tests presently do not excersize UIs, just a set of functional API
  calls. Can consider adding Selenium tests for browser emulation
  in the future.

## Running tests

You can run tests locally. The requirements are listed in
requirements.txt, so if needed first install via pip:

```
pip install -r requirements.txt
```

Then, we create a pointer to the sciserver base url to be tested, for
example:

```
echo https://kubetest.sciserver.org/ak-1 > sciserverbase
```

And then running the full suite in order:

```
nose2 --config nose2.config test_preflight test_register test_api test_logout
```

Or, if you've already run the **test_register** tests, then you can
run others independently:

```
nose2 --config nose2.config test_api
```

The nose2 config is setup to generate an html report at report.html
for a friendly view of results.