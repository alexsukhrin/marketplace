build:
	docker build -t marketplace-image .

deploy:
	docker run --env-file=env -d -it -p 80:4000 --rm --name marketplace-app marketplace-image

test:
	lein with-profile test test
