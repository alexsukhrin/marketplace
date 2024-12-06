FROM clojure:lein

COPY . /usr/src/app

WORKDIR /usr/src/app

EXPOSE 4000

CMD ["lein", "run"]
