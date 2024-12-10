build:
	docker build -t marketplace-image .

deploy:
	docker run --env-file=env -d -it -p 80:4000 --rm --name marketplace-app marketplace-image

test:
	lein with-profile test test :only marketplace.core-test/test-register-user

stop:
	docker stop $(docker ps -q)

remove:
	docker rm $(docker ps -a -q)

images:
	docker rmi $(docker images -q)
