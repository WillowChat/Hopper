# Hopper
Kotlin/JVM IRC bouncer and multiplexer. Provides an HTTP API for reading and updating state. Uses websockets for pushing state change to clients.

Inspired by [Possel](https://github.com/possel/possel).

This project is managed [on Trello](https://trello.com/b/KgFspfjh/hopper). It's in active development - probably not suitable for use before version 1.0!

[![trello](https://img.shields.io/badge/trello-%F0%9F%93%8B-blue.svg)](https://trello.com/b/KgFspfjh/hopper) [![patreon](https://img.shields.io/badge/patreon-%F0%9F%A5%95-orange.svg)](https://crrt.io/patreon) [![codecov](https://codecov.io/gh/WillowChat/Hopper/branch/develop/graph/badge.svg)](https://codecov.io/gh/WillowChat/Hopper)

## Uses
* [Warren](https://github.com/carrotcodes/warren) for IRC state management
* [Spark](https://github.com/perwendel/spark) for web framework and websockets

## Support

<a href="https://patreon.com/carrotcodes"><img src="https://s3.amazonaws.com/patreon_public_assets/toolbox/patreon.png" align="left" width="160" ></a>
You can support the development of this bouncer through [Patreon](https://crrt.io/patreon) 🎉.

## Code License
The source code of this project is licensed under the terms of the ISC license, listed in the [LICENSE](LICENSE.md) file. A concise summary of the ISC license is available at [choosealicense.org](http://choosealicense.com/licenses/isc/).

## Building
This project uses Gradle and IntelliJ IDEA for pretty easy setup and building:
* **Building**: `./gradlew clean build`
* **Running**: `./gradlew clean run`
