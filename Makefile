VERSION = 1.3

LIBS = libbzdev-base.jar libbzdev-graphics.jar libbzdev-math.jar \
	libbzdev-obnaming.jar libbzdev-ejws.jar libbzdev.jar libosgbatik.jar

epi.jar: $(LIBS) epi/epi.mf epi/Server.java epi/model.html \
		epi/emodel/Adapter.java 
	(cd epi ; make)

docker: epi.jar
	docker build --tag wtzbzdev/epimodel:$(VERSION) .

start:
	docker run --publish 80:80 --detach --name epi \
		wtzbzdev/epimodel:$(VERSION)


stop:
	docker stop epi
	docker rm epi

libbzdev-base.jar: /usr/share/java/libbzdev-base.jar
	cp $< $@

libbzdev-graphics.jar: /usr/share/java/libbzdev-graphics.jar
	cp $< $@

libbzdev-math.jar: /usr/share/java/libbzdev-math.jar
	cp $< $@

libbzdev-obnaming.jar: /usr/share/java/libbzdev-obnaming.jar
	cp $< $@

libbzdev-ejws.jar: /usr/share/java/libbzdev-ejws.jar
	cp $< $@

libbzdev.jar: /usr/share/java/libbzdev.jar
	cp $< $@

libosgbatik.jar: /usr/share/java/libosgbatik.jar
	cp $< $@

libs: $(LIBS)

clean:
	rm $(LIBS) epi.jar
	(cd epi; make clean)
