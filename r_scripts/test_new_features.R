#libraries
tryCatch(library(randomForest), error = function(e){install.packages("randomForest")}, finally = library(randomForest))

#correct directory
setwd("C:\\Users\\kapelner\\workspace\\GemIdent_GPL_v2")
FRAME_RATE = 60 #per second

#load data in
X = read.csv("data_out.csv")
X = X[, -1] #first column is blank so kill it
head(X)

#submatrices for each class
X_y0 = X[X$y == 0, ]
X_y1 = X[X$y == 1, ]
feature_names = colnames(X)

par(mfrow = c(2, 1))
for (i in 1 : (ncol(X) - 1)){
	hist(X_y0[, i], br = 100, main = paste("feature", feature_names[i], "y = 0"))
	hist(X_y1[, i], br = 100, main = paste("feature", feature_names[i], "y = 1"))
	Sys.sleep(1 / FRAME_RATE)
}

par(mfrow = c(1, 1))
for (i in 1 : (ncol(X) - 1)){
	plot(X[, i], jitter(X$y), main = paste("feature", feature_names[i], "on y")) #subtract one for the factor
	Sys.sleep(1 / FRAME_RATE)
}

WEIGHTS_ON_ERRORS_RF = c(1, 1)
NUM_TREES_RF = 500
#convert y to a factor
X$y = as.factor(X$y)
rf = randomForest(y ~ ., X, classwt = WEIGHTS_ON_ERRORS_RF, ntree = NUM_TREES_RF)
barplot(t(rf$importance), las=2, cex.names=0.6, main = paste("importances for RF classifier")) #importances graphed
rf

