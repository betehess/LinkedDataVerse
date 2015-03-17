# Example application written in Scala.js

This is a barebone example of an application written in
[Scala.js](https://www.scala-js.org/).

## Get started

To get started, open `sbt` in this example project, and execute the task
`fastOptJS`. This creates the file `target/scala-2.11/example-fastopt.js`.
You can now open `index-fastopt.html` in your favorite Web browser! (Requires
that you disable CORS requirements to do cross-domain requests: for example,
you need to run Chrome with the --disable-web-security flag)

During development, it is useful to use `~fastOptJS` in sbt, so that each
time you save a source file, a compilation of the project is triggered.
Hence only a refresh of your Web page is needed to see the effects of your
changes.

## Run the tests

To run the test suite, execute the task `test`. If you have installed
[Node.js](http://nodejs.org/), you can also run `fastOptStage::test` which is
faster.

## The fully optimized version

For ultimate code size reduction, use `fullOptJS`. This will take several
seconds to execute, so typically you only use this for the final, production
version of your application. While `index-fastopt.html` refers to the
JavaScript emitted by `fastOptJS`, `index.html` refers to the optimized
JavaScript emitted by `fullOptJS`.


## Vis controls

**Keys**

w/s/a/d:            pan up/down/left/right
q/e:                pan in/out
1/2/3/4             move to document head (if loaded)
enter               start from new URI

**Mouse**

left click:         select item & zoom.
right click:        select item

Clicking a select item loads the URI, or open/closes a BNode.

left click & drag:  rotate around selected item
shift & drag:       pan
right click & drag: pan
scroll wheel:       zoom

