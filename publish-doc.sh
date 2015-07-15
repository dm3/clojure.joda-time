#!/usr/bin/env sh
lein doc \
    && git checkout gh-pages && rm -rf ./css ./js *.html && mv ./doc/* .\
    && git add . && git commit -am "Documentation update" \
    && git push -u origin gh-pages && \
    cd ..
