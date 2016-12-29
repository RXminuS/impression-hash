FROM frekele/gradle

# Create app directory
RUN mkdir -p /app
WORKDIR /app

# Bundle app source
COPY . /app

EXPOSE 50051
CMD [ "gradle", "server" ]
