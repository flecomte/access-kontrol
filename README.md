# Access Kontrol
Helpers to create a simple Access Control in kotlin

[![Tests](https://github.com/flecomte/access-control/actions/workflows/tests.yml/badge.svg)](https://github.com/flecomte/access-control/actions/workflows/tests.yml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=flecomte_access-control&metric=coverage)](https://sonarcloud.io/dashboard?id=flecomte_access-control)

[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=flecomte_access-control&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=flecomte_access-control)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=flecomte_access-control&metric=ncloc)](https://sonarcloud.io/dashboard?id=flecomte_access-control)


## Example

Define AC
```kotlin
class AccessControlSample : AccessControl() {
    /** The user can view the object if it is connected and if it is the creator */
    fun canView(myObject: MyObject, user: User?): AccessResponse {
        return if (user != null && myObject.createdBy == user) {
            granted(message = "OK") // the message if optional on granted
        } else {
            denied(message = "You must be the creator", code = "creator.ko")
        }
    }

    fun canView(myObjects: List<MyObject>, user: User?): AccessResponses {
        return canAll(myObjects) { canView(it, user) }
    }
}
```

Usage
```kotlin
AccessControlSample().canView(MyObject(), User()).let { response ->
    response.message // "OK"
    response.decision == AccessDecision.GRANTED // true
}


try {
    AccessControlSample().canView(MyObject(), User()).assert() // throw exception if no access
} catch (e: AccessDeniedException) {
    e.getFirstMessage() // the access denied message: "You must be the creator"
    e.first.code // the access denied code: "creator.ko"
}


AccessControlSample().canView(MyObject(), User()).toBoolean() // return true if access is granted
```