resolvers in ThisBuild += "lightbend-commercial-mvn" at
  "https://repo.lightbend.com/pass/a23A5aWW3tx9tFTjCb26RMsDMmaq3PIIOogqS1uRflkdph_q/commercial-releases"
resolvers in ThisBuild += Resolver.url("lightbend-commercial-ivy",
  url("https://repo.lightbend.com/pass/a23A5aWW3tx9tFTjCb26RMsDMmaq3PIIOogqS1uRflkdph_q/commercial-releases"))(Resolver.ivyStylePatterns)