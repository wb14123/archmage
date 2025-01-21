

## Publish

For maintainers only

``
sbt +publishLocal

// only for non snapshot version
// Sonatype Central currently does not support publishing snapshots
sbt +publishSigned
sbt sonatypeBundleRelease
`
