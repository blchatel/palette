function [ lab ] = findBoundary( source, dir, l, r )
%findBoundary Find the Cb point 
%   Detailed explanation goes here
lab = source;
for it=1:4
   factor = 0.5 * (l+r);
   lab = lab + factor*dir;
   testRGB = lab2rgb(lab);
   
   if outBoundary(testRGB)
       r = factor;
   else
       l = factor;
   end
end

end

%% Function check if out of boundary
function [ isOut ] = outBoundary( testRGB )
    th = 0.5;
    isOut = testRGB(1) < -th | testRGB(1) > 255+th | testRGB(2) < -th | testRGB(2) > 255+th | testRGB(3) < -th | testRGB(3) > 255+th ;
end