// Flute Bore Profile
lengths = [0, 3, 4, 6, 8, 11, 13, 17, 29, 35, 42, 47, 51, 60, 65, 72, 79, 84, 96, 102, 107, 112, 117, 123, 130, 139, 145, 151, 155, 161, 165, 173, 177, 183, 187, 188, 193, 200, 205, 207, 209];
diameters = [18.9, 18.8, 18.7, 18.6, 18.5, 18.4, 18.3, 18.2, 18.1, 18.0, 17.9, 17.8, 17.7, 17.6, 17.5, 17.4, 17.3, 17.2, 17.1, 17.0, 16.9, 16.8, 16.7, 16.6, 16.5, 16.4, 16.3, 16.2, 16.1, 16.0, 15.9, 15.8, 15.7, 15.6, 15.5, 15.4, 15.3, 15.2, 15.3, 15.4, 15.5];

module flute_bore() {
    // Create 2D profile of half the bore
    profile_points = [
        for(i = [0:len(lengths)-1]) [diameters[i]/2, lengths[i]],
        for(i = [len(lengths)-1:-1:0]) [0, lengths[i]]
    ];
    
    // Rotate extrude to create 3D bore
    rotate_extrude($fn = 64) {
        polygon(profile_points);
    }
}

// Render the bore
flute_bore();

// Optional: Add a cutaway view for inspection
*color("red", 0.3) translate([-25, 0, 0]) cube([50, 20, 250]);