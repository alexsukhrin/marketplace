build:
	docker build -t marketplace-image .

deploy:
	docker run --env-file=env -d -it -p 4000:4000 --rm --name marketplace-app marketplace-image
