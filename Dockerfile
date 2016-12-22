FROM frekele/gradle

# Create app directory
RUN mkdir -p /app
WORKDIR /app

# Bundle app source
COPY . /app

EXPOSE 3000
CMD [ "gradle", "server" ]
