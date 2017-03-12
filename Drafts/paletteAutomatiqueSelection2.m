%constant used
palette_size = 1; 

% Load images
img = imread('images/bridge.png');

nrows = size(img,1);
ncols = size(img,2);

img = img(1:10:nrows,1:10:ncols, :);

nrows = size(img,1);
ncols = size(img,2);

% convert into La*b* domain
lab_img = rgb2lab(img); 

% convert into 3D data point 
data_img = reshape(lab_img, nrows*ncols, 3);
[numInst,numDims] = size(data_img);

%# K-means clustering
%# (K: number of clusters, G: assigned groups, C: cluster centers)
K = palette_size + 1;
[G,C] = kmeans(data_img, K, 'distance','sqEuclidean', 'start','sample');

% sort C by luminance the lighter at the beginning and darker at the end
sortedC = sortrows(C, 1);

rgbPalette = lab2rgb(sortedC)*255;

figure(1);
imshow(img);

figure(2);
for k=2:K

    color = zeros(200, 200, 3);
    
    color(:,:,1) = zeros(200) + rgbPalette(k, 1);
    color(:,:,2) = zeros(200) + rgbPalette(k, 2);
    color(:,:,3) = zeros(200) + rgbPalette(k, 3);
    
    subplot(1,K-1,k-1);
    imshow(uint8(color));
    
end


%# show points and clusters (color-coded)
%clr = lines(K);
%figure, hold on
%scatter3(X(:,1), X(:,2), X(:,3), 36, clr(G,:), 'Marker','.')
%scatter3(C(:,1), C(:,2), C(:,3), 100, clr, 'Marker','o', 'LineWidth',3)
%hold off
%view(3), axis vis3d, box on, rotate3d on
%xlabel('x'), ylabel('y'), zlabel('z')


% imagine we want to change one of the palette's color too a
% redish color (200, 0, 0)
newRGB = [1,0,0];
newLAB = rgb2lab(newRGB);
P = K-1;
palette = sortedC(2:K, :);

i = 1; % The index of the color to change

%% Here we transfer the Luminance first in the Palette
oldPalette = palette;
delta = palette(i, 1) - newLAB(1);
oldLAB = palette(i);

% Update the palette's color
for j=1:P
    if (j ~= i)
        if palette(j, 1) < palette(i, 1)
            palette(j, 1) = newLAB(1) - smoothL(delta, palette(i, 1)-palette(j, 1));
        else
            palette(j, 1) = newLAB(1) + smoothL(-delta, palette(j, 1)-palette(i, 1));
        end
    end
end

% Update the chosen color
palette(i, 1) = newLAB(1);
palette(i, 2) = newLAB(2);
palette(i, 3) = newLAB(3);

diff = oldPalette - palette;

rgbPalette = lab2rgb(palette)*255;
% To be in boundaries
for j=1:P
    rgbPalette(j, 1) = min( 255, max(rgbPalette(j, 1), 0));
    rgbPalette(j, 2) = min( 255, max(rgbPalette(j, 2), 0));
    rgbPalette(j, 3) = min( 255, max(rgbPalette(j, 3), 0));
end

figure(3);
for k=1:P
    
    color = zeros(200, 200, 3);
    
    color(:,:,1) = zeros(200) + rgbPalette(k, 1);
    color(:,:,2) = zeros(200) + rgbPalette(k, 2);
    color(:,:,3) = zeros(200) + rgbPalette(k, 3);
    
    subplot(1,P,k);
    imshow(uint8(color));
    
end

%% Now we transfer to every pixel
% We go on the picture to apply the transfer
image = zeros(nrows, ncols, 3);
for w=1:nrows
    for h=1:ncols
        Lab = lab_img(w, h, :);
        
        if Lab == oldLAB
            Lab = newLab;
        else
            Cb = findBoundary(oldPalette(i, :), diff, 0, 1);
            xb = findBoundary(Lab, diff, 0, 10);
            d = labDistance(Lab, xb) / labDistance(oldPalette(i, :), Cb) * labDistance(oldPalette(i,:), palette(i,:));
                
            Lab = interpolate(Lab, xb, d);
        end
        lab_img(w, h, 1) = Lab(1);
        lab_img(w, h, 2) = Lab(2);
        lab_img(w, h, 3) = Lab(3);
        
        rgb = lab2rgb(Lab)*255;
        r = min(255, max(0, rgb(1)));
        g = min(255, max(0, rgb(2)));
        b = min(255, max(0, rgb(3)));
        rgb = [r g b];
        
        image(w,h, 1) = rgb(1);
        image(w,h, 2) = rgb(2);
        image(w,h, 3) = rgb(3);
        
    end
end



figure(4)
imshow(image)

%% Function that find a point in the segment with left and right limit given and a factor
function [ inter ] = interpolate( l, r, factor)
    inter = l * factor + r * (1-factor);
end



