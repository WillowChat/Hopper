# Hopper
Kotlin IRC bouncer and multiplexer. Provides an HTTP API for reading and updating state. Uses websockets for pushing state change to clients.

Inspired by [Possel](https://github.com/possel/possel).

Written for fun, learning and use in personal projects.

## Project goals
* Document HTTP API and websocket events
* Authentication (log in, log out, auth tokens)
* Networks (adding, removing, fetching)
* Buffers (adding, removing, fetching)
* Lines (adding, fetching)
* Live state (networks, buffers, memberships, lines)

## Uses
* [Warren](https://github.com/carrotcodes/warren) for IRC state management
* [Spark](https://github.com/perwendel/spark) for web framework and websockets

## Code License
The source code of this project is licensed under the terms of the ISC license, listed in the [LICENSE](LICENSE.md) file. A concise summary of the ISC license is available at [choosealicense.org](http://choosealicense.com/licenses/isc/).

## Building
This project uses Gradle and IntelliJ IDEA for pretty easy setup and building. There are better guides around the internet for using them, and I don't do anything particularly special.

The general idea:
* **Setup**: `./gradlew clean idea`
* **Building**: `./gradlew build`
* **Producing an all-in-one Jar**: `./gradlew build shadowJar`

If you run in to odd Gradle issues, doing `./gradlew clean` usually fixes it.
