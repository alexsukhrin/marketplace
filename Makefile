build:
	docker build -t marketplace-image .

deploy:
	docker run --env-file=env -it -p 80:4000 --rm --name marketplace-app marketplace-image
