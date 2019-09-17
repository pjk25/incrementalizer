FROM clojure:tools-deps AS BASE

WORKDIR /incrementalizer

RUN curl -LO https://github.com/cloudfoundry/bosh-cli/releases/download/v6.0.0/bosh-cli-6.0.0-linux-amd64
RUN curl -LO https://github.com/pivotal-cf/om/releases/download/1.0.0/om-linux

RUN curl -LO https://github.com/cloudfoundry-incubator/credhub-cli/releases/download/2.5.3/credhub-linux-2.5.3.tgz
RUN tar -xvf credhub-linux-2.5.3.tgz

ADD scripts scripts
ADD src src
ADD resources resources
ADD test test
ADD deps.edn .

RUN ./scripts/test.sh
RUN ./scripts/compile.sh

FROM openjdk:11-jre-slim

COPY --from=BASE /incrementalizer/bosh-cli-6.0.0-linux-amd64 /usr/local/bin/bosh
COPY --from=BASE /incrementalizer/om-linux /usr/local/bin/om
COPY --from=BASE /incrementalizer/credhub /usr/local/bin/credhub
COPY --from=BASE /incrementalizer/target/incrementalizer-1.0.0-SNAPSHOT-standalone.jar /

RUN chmod +x /usr/local/bin/bosh
RUN chmod +x /usr/local/bin/om
RUN chmod +x /usr/local/bin/credhub

CMD ["java" "-cp" "/incrementalizer-1.0.0-SNAPSHOT-standalone.jar" "clojure.main" "-m" "incrementalizer.cli" "min"]
