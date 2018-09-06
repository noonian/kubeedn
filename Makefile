bin-name := kubeedn

.PHONY: clean
clean:
	[ ! -f $(bin-name) ] || rm $(bin-name)

.PHONY: native-image
native-image: $(bin-name)

$(bin-name):
	./bin/native-image

.PHONY: test
test:
	clojure -m kubeedn.main xf -f manifests/nginx.edn

.PHONY: test-native
test-native:
	./$(bin-name) xf -f manifests/nginx.edn
