FROM clojure:tools-deps AS BASE

RUN apt-get update && apt-get install -y zip

WORKDIR /incrementalizer

RUN curl -LO https://github.com/cloudfoundry/bosh-cli/releases/download/v6.0.0/bosh-cli-6.0.0-linux-amd64
RUN curl -LO https://github.com/pivotal-cf/om/releases/download/4.1.0/om-linux-4.1.0

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
COPY --from=BASE /incrementalizer/om-linux-4.1.0 /usr/local/bin/om
COPY --from=BASE /incrementalizer/credhub /usr/local/bin/credhub
COPY --from=BASE /incrementalizer/target/incrementalizer.jar /

RUN chmod +x /usr/local/bin/bosh
RUN chmod +x /usr/local/bin/om
RUN chmod +x /usr/local/bin/credhub

ADD bin/incrementalizer /usr/local/bin
RUN chmod +x /usr/local/bin/incrementalizer

CMD [ "/bin/bash" ]