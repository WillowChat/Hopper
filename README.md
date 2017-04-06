# Hopper
Kotlin/JVM IRC bouncer and multiplexer. Provides an HTTP API for reading and updating state. Uses websockets for pushing state change to clients.

Inspired by [Possel](https://github.com/possel/possel).

[![codecov](https://codecov.io/gh/WillowChat/Hopper/branch/develop/graph/badge.svg)](https://codecov.io/gh/WillowChat/Hopper)

## TODO
* Document HTTP API and websocket events
* Authentication (log in, log out, auth tokens)
* Networks (adding, removing, fetching)
* Buffers (adding, removing, fetching)
* Lines (adding, fetching)
* Live state (networks, buffers, memberships, lines)

## Uses
* [Warren](https://github.com/carrotcodes/warren) for IRC state management
* [Spark](https://github.com/perwendel/spark) for web framework and websockets

## Support

<a href="https://patreon.com/carrotcodes"><img src="https://s3.amazonaws.com/patreon_public_assets/toolbox/patreon.png" align="left" width="160" ></a>
You can support the development of this bouncer through [Patreon](https://patreon.com/carrotcodes) 🎉.

## Code License
The source code of this project is licensed under the terms of the ISC license, listed in the [LICENSE](LICENSE.md) file. A concise summary of the ISC license is available at [choosealicense.org](http://choosealicense.com/licenses/isc/).

## Building
This project uses Gradle and IntelliJ IDEA for pretty easy setup and building:
* **Building**: `./gradlew clean build`
* **Running**: `./gradlew clean run`
