## Easy authentication for spring based apps

## Setup

To use in a spring-app-utils application (one which uses base-app),
simply add the following dependency in your build.gradle file:

```
dependencies {
...
     implementation 'org.sciserver.springapp:authenticator:1.0+'
```

## Configure

The default login-portal path is set for production, and defaults to
prefetching user info for each request, so there should be nothing
required for production use.

In some cases, and testing we may want to try some changes. Some
parameters to try:

Environment:
```
AUTH_PORTAL_URL=https://{}/login-portal/
AUTH_PREFETCH=false
AUTH_CACHE_EXPIRYMS=120000 # In ms
AUTH_CACHE_SIZE=100
```

Or application.properties
```
auth.portal.url=https://{}/login-portal/
auth.prefetch=false
auth.cache.expiryms=120000 # In ms
auth.cache.size=100
```

## Use

To access the authenticated user within an application, simply include
the Auth utility:

```
import org.sciserver.springapp.auth.Auth;
import org.sciserver.authentication.client.AuthenticatedUser
```

And get the user info, for example within your controller:

```
AuthenticatedUser user = Auth.get();
```

This will raise an unauthenticated exception in the case where the
user did not provide a valid token or did not provide a token at all
(with a message indicating which).

And we can get the username and id:

```
user.getUserId();
user.getUserName();
```

## Observe

When either prefetch is set to `true`, or you call `Auth.get()` at
some point during the request lifetime, the authenticator will pass
user information to the logger via request properties, so if you also
use the logging-interceptor, see the additional information in the
service log.
