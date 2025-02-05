build:
	docker build -t marketplace-image .

deploy:
	docker run --restart=on-failure:3 --env-file=env -d -it -p 8032:4000 --name marketplace-app marketplace-image

test:
	lein test :only marketplace.core-test/test-register-user

stop:
	docker stop $(docker ps -q)

remove:
	docker rm $(docker ps -a -q)

images:
	docker rmi $(docker images -q)
