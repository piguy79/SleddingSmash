FROM jenkins:1.565.3

USER root

WORKDIR /opt

RUN wget http://dl.google.com/android/android-sdk_r24.0.2-linux.tgz && tar xvf android-sdk_r24.0.2-linux.tgz \
    && rm android-sdk_r24.0.2-linux.tgz

RUN chown -hR jenkins:jenkins android-sdk-linux
RUN apt-get update && apt-get install -y git-core
RUN apt-get install -y lib32stdc++6 lib32z1

USER jenkins

ENV ANDROID_HOME /opt/android-sdk-linux


RUN $ANDROID_HOME/tools/android update sdk --no-ui

RUN echo "y" | $ANDROID_HOME/tools/android update sdk -u --filter platform-tools,android-21
RUN echo "y" | $ANDROID_HOME/tools/android update sdk -u --all --filter build-tools-20.0.0



ENV ANDROID_HOME /opt/android-sdk-linux