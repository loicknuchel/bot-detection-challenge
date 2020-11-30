# Bot detection challenge

A coding exercice trying to detect bots using HTTP logs.

This project is published as an exemple of pure FP in Scala using [cats](https://typelevel.org/cats), [fs2](https://fs2.io), [http4s](https://http4s.org) and [akka](https://akka.io).

## How to run it

There is three main classes, one to launch the server (`ServerApp`), one to launch a client that feed the server with logs (`ClientApp`) and a last one to glue the together (`Demo`).

The `Demo` class can be launched using `sbt run` so you can see the system in action with a small file.

Other options are available such as:

- `sbt "run remote"` to read input from http://www.almhuette-raith.at/apache-log/access.log
- `sbt "run full"` to read input from src/main/resources/access.log (1 000 000 lines)
- `sbt "run my/path/to/access.log"` to read input from any file (local or remote with path or url)

The server can be launched using `sbt "runMain fr.loicknuchel.botless.server.ServerApp"`.
Optionally, you can specify the port as first parameter (default: 8080).
The (very basic!) API is described in the home page.

The client can be launched using `sbt "runMain fr.loicknuchel.botless.client.ClientApp"`.
Parameters are optional and in order:

- the input file (path or url) (default: src/test/resources/100.log)
- the server uri to call (default: http://localhost:8080)
- the output file (default: target/results/output.log)

## Enjoy

I hope you will enjoy reading the code, feel free to comment or open a pull request if you have any question or if you see a possible improvement.
I will be happy to discuss them ;)
